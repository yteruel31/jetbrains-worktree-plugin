package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.RemoveWorktreeDialog
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil

class RemoveWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return

        val dialog = RemoveWorktreeDialog(project, worktree)
        if (!dialog.showAndGet()) return

        val shouldCloseWindow = dialog.closeWindow
        val shouldDeleteBranch = dialog.deleteBranch
        val branchToDelete = worktree.branchName
        val worktreePath = worktree.path

        if (shouldCloseWindow) {
            closeWorktreeWindow(worktreePath)
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Removing worktree...", false
        ) {
            override fun run(indicator: ProgressIndicator) {
                val service = GitWorktreeService.getInstance(project)

                indicator.text = "Removing worktree..."
                val error = service.removeWorktree(worktreePath)
                if (error != null) {
                    ApplicationManager.getApplication().invokeLater {
                        val choice = Messages.showYesNoDialog(
                            project,
                            "$error\n\nDo you want to force remove? This will discard any uncommitted changes.",
                            "Force Remove Worktree?",
                            "Force Remove",
                            "Cancel",
                            Messages.getWarningIcon()
                        )
                        if (choice == Messages.YES) {
                            forceRemove(project, service, worktreePath, shouldDeleteBranch, branchToDelete)
                        }
                    }
                    return
                }

                deleteBranchIfNeeded(project, service, shouldDeleteBranch, branchToDelete, indicator)
            }
        })
    }

    private fun forceRemove(
        project: Project,
        service: GitWorktreeService,
        worktreePath: String,
        shouldDeleteBranch: Boolean,
        branchToDelete: String?,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Force removing worktree...", false
        ) {
            override fun run(indicator: ProgressIndicator) {
                val error = service.removeWorktree(worktreePath, force = true)
                if (error != null) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, error, "Remove Failed")
                    }
                    return
                }

                deleteBranchIfNeeded(project, service, shouldDeleteBranch, branchToDelete, indicator)
            }
        })
    }

    private fun deleteBranchIfNeeded(
        project: Project,
        service: GitWorktreeService,
        shouldDeleteBranch: Boolean,
        branchToDelete: String?,
        indicator: ProgressIndicator,
    ) {
        if (shouldDeleteBranch && branchToDelete != null) {
            indicator.text = "Deleting branch '$branchToDelete'..."
            val error = service.deleteBranch(branchToDelete)
            if (error != null) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showWarningDialog(
                        project,
                        "Worktree removed, but branch deletion failed:\n$error",
                        "Branch Not Deleted"
                    )
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)
        e.presentation.isEnabled = e.project != null &&
                worktree != null &&
                !worktree.isMainWorktree
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT

    private fun closeWorktreeWindow(worktreePath: String) {
        val normalizedPath = FileUtil.toSystemIndependentName(worktreePath)
        val projectToClose = ProjectManager.getInstance().openProjects.find { p ->
            p.basePath?.let { FileUtil.toSystemIndependentName(it) } == normalizedPath
        } ?: return

        FileDocumentManager.getInstance().saveAllDocuments()
        projectToClose.save()
        ProjectManager.getInstance().closeAndDispose(projectToClose)
    }
}
