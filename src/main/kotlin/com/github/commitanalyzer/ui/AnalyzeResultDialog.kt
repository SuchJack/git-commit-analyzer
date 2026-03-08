package com.github.commitanalyzer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.UIUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.awt.*
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class AnalyzeResultDialog(
    private val project: Project,
    private val commitHash: String,
    private val diffText: String,
    private val analysisText: String,
    private val repository: GitRepository
) : DialogWrapper(project, true) {

    init {
        title = "AI 提交分析 — $commitHash"
        setOKButtonText("关闭")
        setCancelButtonText("Cherry-Pick")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val tabbedPane = JBTabbedPane()

        // --- Tab 1: AI 分析结果 ---
        val analysisPanel = createAnalysisPanel()
        tabbedPane.addTab("AI 分析结果", analysisPanel)

        // --- Tab 2: 原始 Diff ---
        val diffPanel = createDiffPanel()
        tabbedPane.addTab("原始 Diff", diffPanel)

        val root = JPanel(BorderLayout())
        root.preferredSize = Dimension(800, 600)

        val header = createHeaderPanel()
        root.add(header, BorderLayout.NORTH)
        root.add(tabbedPane, BorderLayout.CENTER)

        return root
    }

    private fun createHeaderPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5))
        panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        val hashLabel = JLabel("Commit: ")
        hashLabel.font = hashLabel.font.deriveFont(Font.BOLD)
        panel.add(hashLabel)

        val hashValue = JLabel(commitHash)
        hashValue.foreground = JBColor(Color(0, 102, 204), Color(88, 166, 255))
        panel.add(hashValue)

        return panel
    }

    private fun createAnalysisPanel(): JComponent {
        val html = markdownToHtml(analysisText)
        val textPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            editorKit = HTMLEditorKit().apply {
                styleSheet = createMarkdownStyleSheet()
            }
            background = UIUtil.getPanelBackground()
            foreground = UIUtil.getLabelForeground()
            text = html
            caretPosition = 0
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        return JBScrollPane(textPane)
    }

    private fun markdownToHtml(markdown: String): String {
        val parser = Parser.builder().build()
        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder().build()
        val body = renderer.render(document)
        return """
            <html>
            <head><meta charset="UTF-8"></head>
            <body class="markdown-body">$body</body>
            </html>
        """.trimIndent()
    }

    private fun createMarkdownStyleSheet(): StyleSheet {
        val fg = UIUtil.getLabelForeground()
        val bg = UIUtil.getPanelBackground()
        val codeBg = JBColor(Color(0xE8, 0xE8, 0xE8), Color(0x3C, 0x3F, 0x41))
        val fgHex = colorToHex(fg)
        val bgHex = colorToHex(bg)
        val codeBgHex = colorToHex(codeBg)

        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: 'Microsoft YaHei', sans-serif; font-size: 14px; padding: 8px; color: $fgHex; background-color: $bgHex; }")
        styleSheet.addRule("h1 { font-size: 1.6em; margin-top: 16px; margin-bottom: 8px; }")
        styleSheet.addRule("h2 { font-size: 1.4em; margin-top: 14px; margin-bottom: 6px; }")
        styleSheet.addRule("h3 { font-size: 1.2em; margin-top: 12px; margin-bottom: 4px; }")
        styleSheet.addRule("ul, ol { margin: 8px 0; padding-left: 24px; }")
        styleSheet.addRule("li { margin: 4px 0; }")
        styleSheet.addRule("code { background-color: $codeBgHex; color: $fgHex; padding: 2px 6px; border-radius: 4px; font-family: Consolas, monospace; }")
        styleSheet.addRule("pre { background-color: $codeBgHex; padding: 12px; border-radius: 6px; overflow-x: auto; margin: 12px 0; }")
        styleSheet.addRule("pre code { background: none; padding: 0; }")
        styleSheet.addRule("p { margin: 8px 0; }")
        return styleSheet
    }

    private fun colorToHex(c: Color): String {
        return String.format("#%02x%02x%02x", c.red, c.green, c.blue)
    }

    private fun createDiffPanel(): JComponent {
        val textArea = JTextArea(diffText)
        textArea.isEditable = false
        textArea.font = Font("Consolas", Font.PLAIN, 12)
        textArea.tabSize = 4
        textArea.caretPosition = 0
        textArea.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        return JBScrollPane(textArea)
    }

    override fun createActions(): Array<Action> {
        val closeAction = okAction
        val cherryPickAction = object : AbstractAction("Cherry-Pick 该提交") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                performCherryPick()
            }
        }
        return arrayOf(closeAction, cherryPickAction)
    }

    override fun doCancelAction() {
        super.doCancelAction()
    }

    private fun performCherryPick() {
        val confirmed = Messages.showYesNoDialog(
            project,
            "确定要 Cherry-Pick 提交 $commitHash 到当前分支吗？",
            "确认 Cherry-Pick",
            Messages.getQuestionIcon()
        )
        if (confirmed != Messages.YES) return

        val handler = GitLineHandler(project, repository.root, GitCommand.CHERRY_PICK)
        handler.addParameters(commitHash)
        val result = Git.getInstance().runCommand(handler)

        if (result.success()) {
            Messages.showInfoMessage(project, "Cherry-Pick 成功！", "成功")
            close(OK_EXIT_CODE)
        } else {
            Messages.showErrorDialog(
                project,
                "Cherry-Pick 失败：\n${result.errorOutputAsJoinedString}",
                "Cherry-Pick 失败"
            )
        }
    }
}
