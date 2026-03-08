package com.github.commitanalyzer.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "CommitAnalyzerSettings", storages = [Storage("CommitAnalyzerSettings.xml")])
class CommitAnalyzerSettings : PersistentStateComponent<CommitAnalyzerSettings.State> {

    data class State(
        var apiBaseUrl: String = "https://api.openai.com/v1",
        var apiKey: String = "",
        var model: String = "gpt-4",
        var timeoutSeconds: Int = 120,
        var systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        var maxDiffLength: Int = 30000
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """你是一位资深的代码审查专家。请分析以下 Git 提交的变更，给出：
1. **变更摘要**：用 2-3 句话概括本次提交做了什么
2. **影响范围**：列出受影响的模块/功能
3. **潜在风险**：指出可能的 bug、性能问题、安全隐患等
4. **代码质量**：评估代码风格、可维护性
5. **合并建议**：明确给出「建议合并」或「不建议合并」，并说明理由

请用中文回答，条理清晰。"""

        fun getInstance(project: Project): CommitAnalyzerSettings {
            return project.getService(CommitAnalyzerSettings::class.java)
        }
    }
}
