package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import java.nio.file.Path

class OpenWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return
        val worktreePath = Path.of(worktree.path)

        val settings = project.service<WorktreeSettingsService>()
        if (settings.state.openWorktreeInNewWindow) {
            ProjectUtil.openOrImport(worktreePath)
        } else {
            ProjectUtil.openOrImport(worktreePath, project, false)
        }
    }

    override fun update(e: AnActionEvent) {
        val worktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE)
        e.presentation.isEnabled = e.project != null && worktree != null
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
}
