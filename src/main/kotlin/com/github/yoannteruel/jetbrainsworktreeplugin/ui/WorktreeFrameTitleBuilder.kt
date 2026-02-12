package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.FrameTitleBuilder

class WorktreeFrameTitleBuilder : FrameTitleBuilder() {

    override fun getProjectTitle(project: Project): String {
        val defaultTitle = project.name

        val settings = project.service<WorktreeSettingsService>()
        if (!settings.state.showWorktreeInTitle) {
            return defaultTitle
        }

        val worktreeService = GitWorktreeService.getInstance(project)
        val worktrees = worktreeService.getCachedWorktrees()

        if (worktrees.size <= 1) return defaultTitle

        val projectPath = project.basePath ?: return defaultTitle
        val currentWorktree = worktrees.find { it.path == projectPath }
            ?: return defaultTitle

        val branchLabel = currentWorktree.branchName ?: "(detached)"
        return "$defaultTitle [worktree: $branchLabel]"
    }

    override fun getFileTitle(project: Project, file: VirtualFile): String {
        return file.presentableName
    }
}
