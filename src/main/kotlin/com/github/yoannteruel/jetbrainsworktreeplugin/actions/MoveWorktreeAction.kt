package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.MoveWorktreeDialog
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class MoveWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return

        val dialog = MoveWorktreeDialog(project, worktree.path)
        if (!dialog.showAndGet()) return

        GitWorktreeService.getInstance(project).moveWorktree(worktree.path, dialog.newPath)
    }

    override fun update(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)
        e.presentation.isEnabled = e.project != null &&
                worktree != null &&
                !worktree.isMainWorktree &&
                !worktree.isLocked
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
