package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class LockWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return
        val service = GitWorktreeService.getInstance(project)

        if (worktree.isLocked) {
            service.unlockWorktree(worktree.path)
        } else {
            val reason = Messages.showInputDialog(
                project,
                "Enter an optional lock reason:",
                "Lock Worktree",
                Messages.getQuestionIcon()
            )
            // User pressed cancel
            if (reason == null) return
            service.lockWorktree(worktree.path, reason.ifBlank { null })
        }
    }

    override fun update(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)
        e.presentation.isEnabled = e.project != null &&
                worktree != null &&
                !worktree.isMainWorktree

        if (worktree != null) {
            e.presentation.text = if (worktree.isLocked) "Unlock Worktree" else "Lock Worktree"
        }
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
