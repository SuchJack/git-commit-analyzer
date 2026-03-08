package com.github.commitanalyzer.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class CommitAnalyzerConfigurable(private val project: Project) : Configurable {

    private var apiBaseUrlField: JTextField? = null
    private var apiKeyField: JPasswordField? = null
    private var modelField: JTextField? = null
    private var timeoutField: JSpinner? = null
    private var maxDiffLengthField: JSpinner? = null
    private var systemPromptArea: JTextArea? = null
    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "Git Commit Analyzer"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }

        var row = 0

        // --- API Base URL ---
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JLabel("API Base URL:"), gbc)
        apiBaseUrlField = JTextField(30)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(apiBaseUrlField, gbc)

        // --- API Key ---
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JLabel("API Key:"), gbc)
        apiKeyField = JPasswordField(30)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(apiKeyField, gbc)

        // --- Model ---
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JLabel("Model:"), gbc)
        modelField = JTextField(30)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(modelField, gbc)

        // --- Timeout ---
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JLabel("超时时间 (秒):"), gbc)
        timeoutField = JSpinner(SpinnerNumberModel(120, 10, 600, 10))
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(timeoutField, gbc)

        // --- Max Diff Length ---
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JLabel("Diff 最大字符数:"), gbc)
        maxDiffLengthField = JSpinner(SpinnerNumberModel(30000, 5000, 200000, 5000))
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(maxDiffLengthField, gbc)

        // --- System Prompt ---
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.NORTHWEST
        panel.add(JLabel("System Prompt:"), gbc)
        systemPromptArea = JTextArea(8, 40).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPane = JScrollPane(systemPromptArea).apply {
            preferredSize = Dimension(400, 160)
        }
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        panel.add(scrollPane, gbc)

        // --- Reset Prompt Button ---
        row++
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.0; gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST
        val resetBtn = JButton("恢复默认 Prompt")
        resetBtn.addActionListener {
            systemPromptArea?.text = CommitAnalyzerSettings.DEFAULT_SYSTEM_PROMPT
        }
        panel.add(resetBtn, gbc)

        val wrapper = JPanel(BorderLayout())
        wrapper.add(panel, BorderLayout.NORTH)
        mainPanel = wrapper
        return wrapper
    }

    override fun isModified(): Boolean {
        val state = CommitAnalyzerSettings.getInstance(project).state
        return apiBaseUrlField?.text != state.apiBaseUrl ||
                String(apiKeyField?.password ?: charArrayOf()) != state.apiKey ||
                modelField?.text != state.model ||
                (timeoutField?.value as? Int) != state.timeoutSeconds ||
                (maxDiffLengthField?.value as? Int) != state.maxDiffLength ||
                systemPromptArea?.text != state.systemPrompt
    }

    override fun apply() {
        val settings = CommitAnalyzerSettings.getInstance(project)
        settings.loadState(CommitAnalyzerSettings.State(
            apiBaseUrl = apiBaseUrlField?.text?.trim() ?: "",
            apiKey = String(apiKeyField?.password ?: charArrayOf()),
            model = modelField?.text?.trim() ?: "gpt-4",
            timeoutSeconds = (timeoutField?.value as? Int) ?: 120,
            maxDiffLength = (maxDiffLengthField?.value as? Int) ?: 30000,
            systemPrompt = systemPromptArea?.text ?: CommitAnalyzerSettings.DEFAULT_SYSTEM_PROMPT
        ))
    }

    override fun reset() {
        val state = CommitAnalyzerSettings.getInstance(project).state
        apiBaseUrlField?.text = state.apiBaseUrl
        apiKeyField?.text = state.apiKey
        modelField?.text = state.model
        timeoutField?.value = state.timeoutSeconds
        maxDiffLengthField?.value = state.maxDiffLength
        systemPromptArea?.text = state.systemPrompt
    }
}
