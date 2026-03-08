package com.github.commitanalyzer.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler

object DiffService {

    private const val MAX_DIFF_LENGTH = 30_000

    fun getDiffForCommit(project: Project, root: VirtualFile, hash: String): Result<String> {
        return try {
            val handler = GitLineHandler(project, root, GitCommand.SHOW)
            handler.setSilent(true)
            handler.addParameters(
                hash,
                "--no-color",
                "--format=format:Commit: %H%nAuthor: %an <%ae>%nDate: %ad%nMessage: %s%n",
                "-p",
                "--stat"
            )

            val result = Git.getInstance().runCommand(handler)

            if (result.success()) {
                var output = result.outputAsJoinedString
                if (output.length > MAX_DIFF_LENGTH) {
                    output = output.substring(0, MAX_DIFF_LENGTH) +
                            "\n\n... [diff 过长，已截断至 ${MAX_DIFF_LENGTH} 字符] ..."
                }
                Result.success(output)
            } else {
                Result.failure(RuntimeException(result.errorOutputAsJoinedString))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
