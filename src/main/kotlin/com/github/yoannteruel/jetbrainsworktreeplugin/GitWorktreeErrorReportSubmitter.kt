package com.github.yoannteruel.jetbrainsworktreeplugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus
import com.intellij.util.Consumer
import java.awt.Component
import java.net.URLEncoder

class GitWorktreeErrorReportSubmitter : ErrorReportSubmitter() {

    override fun getReportActionText(): String = "Report to Git Worktree Tool Issue Tracker"

    override fun getPrivacyNoticeText(): String =
        "This will open a pre-filled GitHub issue in your browser. " +
            "Home directory paths are replaced with &lt;HOME&gt;. " +
            "Please review the issue before submitting."

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val event = events.firstOrNull() ?: return false

        val url = buildUrl(event, additionalInfo)
        BrowserUtil.browse(url)
        consumer.consume(SubmittedReportInfo(SubmissionStatus.NEW_ISSUE))
        return true
    }

    private fun buildUrl(event: IdeaLoggingEvent, additionalInfo: String?): String {
        val appInfo = ApplicationInfo.getInstance()
        val pluginVersion = pluginDescriptor.version

        val title = buildTitle(event)
        val description = additionalInfo?.trim()?.ifBlank { null }
            ?: "An unexpected error occurred in the Git Worktree Tool plugin."
        val actual = event.throwableText
            ?.lineSequence()
            ?.firstOrNull()
            ?.take(MAX_TITLE_LENGTH)
            ?: "Unknown error"
        val ideVersion = "${appInfo.fullApplicationName} (${appInfo.build.asString()})"
        val os = "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"
        val logs = event.throwableText
            ?.let { truncate(sanitizePii(it), MAX_STACKTRACE_LENGTH) }
            ?: ""

        val params = linkedMapOf(
            "template" to "bug_report.yml",
            "title" to title,
            "description" to description,
            "steps" to "Exception occurred during plugin execution.",
            "expected" to "No error.",
            "actual" to actual,
            "ide-version" to ideVersion,
            "plugin-version" to pluginVersion,
            "os" to os,
            "logs" to logs,
        )

        val url = buildString {
            append(GITHUB_ISSUES_URL)
            append('?')
            append(params.entries.joinToString("&") { (key, value) ->
                "$key=${encode(value)}"
            })
        }

        if (url.length <= MAX_URL_LENGTH) return url

        // If URL is too long, truncate the logs field to fit
        val urlWithoutLogs = buildString {
            append(GITHUB_ISSUES_URL)
            append('?')
            append(params.entries
                .filter { it.key != "logs" }
                .joinToString("&") { (key, value) -> "$key=${encode(value)}" })
            append("&logs=")
        }
        val availableForLogs = MAX_URL_LENGTH - urlWithoutLogs.length
        val truncatedLogs = truncateToEncodedLength(logs, availableForLogs)
        return "$urlWithoutLogs$truncatedLogs"
    }

    private fun buildTitle(event: IdeaLoggingEvent): String {
        val message = event.message?.take(MAX_TITLE_LENGTH)
            ?: event.throwableText
                ?.lineSequence()
                ?.firstOrNull()
                ?.take(MAX_TITLE_LENGTH)
            ?: "Unknown error"
        return "Bug: $message"
    }

    companion object {
        private const val GITHUB_ISSUES_URL =
            "https://github.com/yteruel31/jetbrains-worktree-plugin/issues/new"
        private const val MAX_URL_LENGTH = 8000
        private const val MAX_STACKTRACE_LENGTH = 3000
        private const val MAX_TITLE_LENGTH = 100

        internal fun sanitizePii(text: String): String {
            val home = System.getProperty("user.home") ?: return text
            return text.replace(home, "<HOME>")
        }

        internal fun truncate(text: String, maxLength: Int): String {
            if (text.length <= maxLength) return text
            return text.take(maxLength) + "\n... (truncated)"
        }

        private fun encode(text: String): String =
            URLEncoder.encode(text, Charsets.UTF_8)

        private fun truncateToEncodedLength(rawText: String, maxEncodedLength: Int): String {
            var candidate = rawText
            while (encode(candidate).length > maxEncodedLength && candidate.isNotEmpty()) {
                candidate = candidate.take((candidate.length * 0.8).toInt())
            }
            return if (candidate.length < rawText.length) {
                encode(candidate + "\n... (truncated)")
            } else {
                encode(candidate)
            }
        }
    }
}
