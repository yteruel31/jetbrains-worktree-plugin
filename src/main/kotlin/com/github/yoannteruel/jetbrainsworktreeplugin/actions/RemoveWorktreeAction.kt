package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class RemoveWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return

        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to remove the worktree at '${worktree.path}'?",
            "Remove Worktree",
            Messages.getQuestionIcon()
        )
        if (result != Messages.YES) return

        GitWorktreeService.getInstance(project).removeWorktree(worktree.path)
    }

    override fun update(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)
        e.presentation.isEnabled = e.project != null &&
                worktree != null &&
                !worktree.isMainWorktree
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
