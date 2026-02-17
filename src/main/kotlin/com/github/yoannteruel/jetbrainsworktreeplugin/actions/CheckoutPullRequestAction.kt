package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.WorktreeSyncService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.CheckoutPRDialog
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import git4idea.repo.GitRepositoryManager
import java.nio.file.Path

class CheckoutPullRequestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = GitWorktreeService.getInstance(project)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Detecting remote provider...", true) {
            private var provider: GitWorktreeService.RemoteProvider? = null

            override fun run(indicator: ProgressIndicator) {
                provider = service.detectRemoteProvider()
            }

            override fun onSuccess() {
                val detectedProvider = provider
                if (detectedProvider == null) {
                    Messages.showWarningDialog(
                        project,
                        "Could not detect a supported remote provider (GitHub, GitLab, or Bitbucket) from the 'origin' remote URL.",
                        "Unsupported Remote"
                    )
                    return
                }

                val dialog = CheckoutPRDialog(project, detectedProvider)
                if (!dialog.showAndGet()) return

                val prNumber = dialog.prNumber
                val path = dialog.worktreePath
                val shouldSync = dialog.syncFiles
                val shouldRunCommand = dialog.runPostCreationCommand
                val shouldOpen = dialog.openInNewWindow

                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking out pull request...", false) {
                    private var creationSucceeded = false

                    override fun run(indicator: ProgressIndicator) {
                        indicator.text = "Fetching PR #$prNumber..."
                        val localBranch = service.fetchPullRequest(prNumber, detectedProvider)
                        if (localBranch == null) {
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                Messages.showErrorDialog(
                                    project,
                                    "Failed to fetch PR #$prNumber from ${detectedProvider.label}. Check the IDE log for details.",
                                    "Fetch Failed"
                                )
                            }
                            return
                        }

                        indicator.text = "Creating worktree..."
                        service.addWorktree(path, localBranch, false, null)
                        creationSucceeded = true

                        if (shouldSync) {
                            indicator.text = "Syncing files to worktree..."
                            WorktreeSyncService.getInstance(project).syncToWorktree(path)
                        }

                        if (shouldRunCommand) {
                            val command = WorktreeSettingsService.getInstance(project).state.postCreationCommand
                            if (!command.isNullOrBlank()) {
                                indicator.text = "Running post-creation command..."
                                service.runPostCreationCommand(path, command)
                            }
                        }
                    }

                    override fun onSuccess() {
                        if (creationSucceeded && shouldOpen) {
                            ProjectUtil.openOrImport(Path.of(path), project, true)
                        }
                    }
                })
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
