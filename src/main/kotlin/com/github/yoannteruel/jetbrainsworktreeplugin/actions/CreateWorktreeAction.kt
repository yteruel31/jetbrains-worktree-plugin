package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.IdeaSyncService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.CreateWorktreeDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import git4idea.repo.GitRepositoryManager

class CreateWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = CreateWorktreeDialog(project)
        if (!dialog.showAndGet()) return

        val path = dialog.worktreePath
        val branch = dialog.branchName
        val createNew = dialog.createNewBranch
        val base = dialog.baseBranch

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating worktree...", false) {
            override fun run(indicator: ProgressIndicator) {
                val service = GitWorktreeService.getInstance(project)
                service.addWorktree(path, branch, createNew, base)

                val settings = WorktreeSettingsService.getInstance(project)
                if (settings.state.autoSyncIdea) {
                    IdeaSyncService.getInstance(project).syncToWorktree(path)
                }
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
