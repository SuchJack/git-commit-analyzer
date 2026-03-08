package com.github.commitanalyzer.service

import com.github.commitanalyzer.config.CommitAnalyzerSettings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

object LlmService {

    private val gson = Gson()

    fun analyze(project: Project, hash: String, diffText: String): Result<String> {
        val settings = CommitAnalyzerSettings.getInstance(project).state
        val apiBaseUrl = settings.apiBaseUrl.trimEnd('/')
        val apiKey = settings.apiKey
        val model = settings.model
        val timeout = settings.timeoutSeconds * 1000
        val systemPrompt = settings.systemPrompt.ifBlank {
            CommitAnalyzerSettings.DEFAULT_SYSTEM_PROMPT
        }

        val userContent = buildUserPrompt(hash, diffText)

        val requestBody = buildRequestBody(model, systemPrompt, userContent)

        return try {
            val url = URI("$apiBaseUrl/chat/completions").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "无响应体"
                return Result.failure(
                    RuntimeException("HTTP $responseCode: $errorBody")
                )
            }

            val responseText = conn.inputStream.bufferedReader().readText()
            val content = parseResponse(responseText)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUserPrompt(hash: String, diffText: String): String {
        return """
以下是 commit $hash 的完整变更内容（git show 输出）：

```
$diffText
```

请按照要求进行分析。
        """.trimIndent()
    }

    private fun buildRequestBody(model: String, systemPrompt: String, userContent: String): String {
        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to userContent)
        )
        val body = mapOf(
            "model" to model,
            "messages" to messages,
            "temperature" to 0.3
        )
        return gson.toJson(body)
    }

    private fun parseResponse(json: String): String {
        val root = JsonParser.parseString(json).asJsonObject
        val choices = root.getAsJsonArray("choices")
        if (choices == null || choices.size() == 0) {
            return "模型返回为空，请检查配置。"
        }
        val firstChoice = choices[0].asJsonObject
        val message = firstChoice.getAsJsonObject("message")
        return message?.get("content")?.asString ?: "无法解析模型返回内容。"
    }
}
