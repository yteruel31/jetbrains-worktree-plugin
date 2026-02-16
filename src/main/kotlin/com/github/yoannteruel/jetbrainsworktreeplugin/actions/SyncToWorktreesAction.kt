package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.WorktreeSyncService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages

class SyncToWorktreesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading worktrees...", false) {
            override fun run(indicator: ProgressIndicator) {
                val worktrees = GitWorktreeService.getInstance(project).listWorktrees()
                val targets = worktrees.filter { !it.isMainWorktree }

                ApplicationManager.getApplication().invokeLater {
                    if (targets.isEmpty()) {
                        Messages.showInfoMessage(project, "No linked worktrees to sync to.", "Sync to Worktrees")
                        return@invokeLater
                    }

                    val listing = targets.joinToString("\n") { "  - ${it.path}" }
                    val result = Messages.showYesNoDialog(
                        project,
                        "Sync enabled entries to the following worktrees?\n\n$listing",
                        "Sync to Worktrees",
                        Messages.getQuestionIcon()
                    )
                    if (result != Messages.YES) return@invokeLater

                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Syncing to worktrees...", false) {
                        override fun run(indicator: ProgressIndicator) {
                            WorktreeSyncService.getInstance(project).syncToAllWorktrees()
                        }
                    })
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
