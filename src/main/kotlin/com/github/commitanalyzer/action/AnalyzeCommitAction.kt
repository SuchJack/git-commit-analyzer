package com.github.commitanalyzer.action

import com.github.commitanalyzer.config.CommitAnalyzerSettings
import com.github.commitanalyzer.service.DiffService
import com.github.commitanalyzer.service.LlmService
import com.github.commitanalyzer.ui.AnalyzeResultDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.vcs.log.VcsLogDataKeys
import git4idea.repo.GitRepositoryManager
import javax.swing.SwingUtilities

class AnalyzeCommitAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val log = e.getData(VcsLogDataKeys.VCS_LOG)
        if (log == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val selectedCommits = log.selectedCommits
        e.presentation.isEnabledAndVisible = selectedCommits.size == 1
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val log = e.getData(VcsLogDataKeys.VCS_LOG) ?: return
        val selectedCommits = log.selectedCommits
        if (selectedCommits.size != 1) return

        val commitId = selectedCommits[0]
        val hash = commitId.hash.asString()
        val root = commitId.root

        val settings = CommitAnalyzerSettings.getInstance(project)
        if (settings.state.apiKey.isBlank()) {
            Messages.showWarningDialog(
                project,
                "请先在 Settings → Tools → Git Commit Analyzer 中配置大模型 API Key。",
                "未配置 API Key"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "AI 分析提交中...", true) {
            override fun run(indicator: ProgressIndicator) {
                val repoManager = GitRepositoryManager.getInstance(project)
                val repo = repoManager.getRepositoryForRoot(root)
                if (repo == null) {
                    showError(project, "未找到 Git 仓库")
                    return
                }

                indicator.isIndeterminate = false
                indicator.fraction = 0.1
                indicator.text = "获取提交变更..."

                val diffResult = DiffService.getDiffForCommit(project, root, hash)
                if (diffResult.isFailure) {
                    showError(project, "获取 diff 失败: ${diffResult.exceptionOrNull()?.message}")
                    return
                }
                val diffText = diffResult.getOrThrow()

                indicator.fraction = 0.4
                indicator.text = "调用大模型分析..."

                val llmResult = LlmService.analyze(project, hash, diffText)
                if (llmResult.isFailure) {
                    showError(project, "大模型调用失败: ${llmResult.exceptionOrNull()?.message}")
                    return
                }
                val analysisText = llmResult.getOrThrow()

                indicator.fraction = 1.0
                indicator.text = "分析完成"

                SwingUtilities.invokeLater {
                    AnalyzeResultDialog(project, hash, diffText, analysisText, repo).show()
                }
            }
        })
    }

    private fun showError(project: Project, message: String) {
        SwingUtilities.invokeLater {
            Messages.showErrorDialog(project, message, "分析失败")
        }
    }
}
