package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.IdeaSyncService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.CheckoutPRDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import git4idea.repo.GitRepositoryManager

class CheckoutPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = GitWorktreeService.getInstance(project)

        val provider = service.detectRemoteProvider()
        if (provider == null) {
            Messages.showWarningDialog(
                project,
                "Could not detect a supported remote provider (GitHub, GitLab, or Bitbucket) from the 'origin' remote URL.",
                "Unsupported Remote"
            )
            return
        }

        val dialog = CheckoutPRDialog(project, provider)
        if (!dialog.showAndGet()) return

        val prNumber = dialog.prNumber
        val path = dialog.worktreePath

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking out pull request...", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Fetching PR #$prNumber..."
                val localBranch = service.fetchPullRequest(prNumber, provider)
                if (localBranch == null) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Failed to fetch PR #$prNumber from ${provider.label}. Check the IDE log for details.",
                            "Fetch Failed"
                        )
                    }
                    return
                }

                indicator.text = "Creating worktree..."
                service.addWorktree(path, localBranch, false, null)

                val settings = WorktreeSettingsService.getInstance(project)
                if (settings.state.autoSyncIdea) {
                    indicator.text = "Syncing .idea settings..."
                    IdeaSyncService.getInstance(project).syncToWorktree(path)
                }

                if (settings.state.postCreationCommandEnabled && !settings.state.postCreationCommand.isNullOrBlank()) {
                    indicator.text = "Running post-creation command..."
                    service.runPostCreationCommand(path, settings.state.postCreationCommand!!)
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null &&
                GitRepositoryManager.getInstance(project).repositories.isNotEmpty()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
