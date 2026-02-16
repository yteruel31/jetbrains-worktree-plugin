package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.WorktreeSyncService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.CreateWorktreeFromCommitDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.vcs.log.VcsLogDataKeys

class CreateWorktreeFromCommitAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION) ?: return
        val commits = selection.commits
        if (commits.isEmpty()) return

        val commitHash = commits.first().hash.asString()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading branches...", true) {
            private lateinit var availableBranches: List<String>

            override fun run(indicator: ProgressIndicator) {
                availableBranches = GitWorktreeService.getInstance(project).getAvailableBranches()
            }

            override fun onSuccess() {
                val dialog = CreateWorktreeFromCommitDialog(project, commitHash, availableBranches)
                if (!dialog.showAndGet()) return

                val path = dialog.worktreePath
                val createNew = dialog.createNewBranch
                val branch = dialog.branchName

                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating worktree...", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val service = GitWorktreeService.getInstance(project)
                        if (createNew && branch.isNotBlank()) {
                            service.addWorktree(path, branch, true, commitHash)
                        } else {
                            service.addWorktree(path, commitHash, false, null)
                        }

                        val settings = WorktreeSettingsService.getInstance(project)
                        if (settings.state.autoSync) {
                            indicator.text = "Syncing files to worktree..."
                            WorktreeSyncService.getInstance(project).syncToWorktree(path)
                        }

                        if (settings.state.postCreationCommandEnabled && !settings.state.postCreationCommand.isNullOrBlank()) {
                            indicator.text = "Running post-creation command..."
                            service.runPostCreationCommand(path, settings.state.postCreationCommand!!)
                        }
                    }
                })
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION)
        e.presentation.isEnabledAndVisible = e.project != null &&
                selection != null &&
                selection.commits.isNotEmpty()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
