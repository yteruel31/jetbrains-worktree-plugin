package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.nio.file.Path

class OpenWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return
        ProjectUtil.openOrImport(Path.of(worktree.path))
    }

    override fun update(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)
        e.presentation.isEnabled = e.project != null && worktree != null
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
