package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.WorktreeSyncService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.CreateWorktreeDialog
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import git4idea.repo.GitRepositoryManager
import java.nio.file.Path

class CreateWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectedWorktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading branches...", true) {
            private lateinit var availableBranches: List<String>
            private lateinit var allBranches: List<String>
            private var defaultBranchName: String? = null

            override fun run(indicator: ProgressIndicator) {
                val service = GitWorktreeService.getInstance(project)
                availableBranches = service.getAvailableBranches()
                allBranches = service.getAllBranches()
                defaultBranchName = service.getDefaultBranchName()
            }

            override fun onSuccess() {
                val defaultBaseBranch = selectedWorktree?.branchName ?: defaultBranchName
                val dialog = CreateWorktreeDialog(project, availableBranches, allBranches, defaultBaseBranch)
                if (!dialog.showAndGet()) return

                val path = dialog.worktreePath
                val branch = dialog.branchName
                val createNew = dialog.createNewBranch
                val base = dialog.baseBranch
                val shouldSync = dialog.syncFiles
                val shouldRunCommand = dialog.runPostCreationCommand
                val shouldOpen = dialog.openInNewWindow

                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating worktree...", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val service = GitWorktreeService.getInstance(project)
                        service.addWorktree(path, branch, createNew, base)

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
                        if (shouldOpen) {
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

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
