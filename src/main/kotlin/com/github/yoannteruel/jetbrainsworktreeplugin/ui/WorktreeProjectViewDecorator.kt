package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.project.Project

class WorktreeProjectViewDecorator(private val project: Project) : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        if (node !is PsiDirectoryNode) return
        if (project.isDisposed) return

        val dir = node.virtualFile ?: return
        val projectPath = project.basePath ?: return
        if (dir.path != projectPath) return

        val worktreeService = GitWorktreeService.getInstance(project)
        val worktrees = worktreeService.getCachedWorktrees()

        if (worktrees.size <= 1) return

        val currentWorktree = worktrees.find { it.path == projectPath } ?: return
        val branchLabel = currentWorktree.branchName ?: "(detached)"
        data.locationString = "[$branchLabel]"
    }
}
