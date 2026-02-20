package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import git4idea.repo.GitRepositoryManager
import java.util.function.Predicate

class WorktreeChangesViewContentProvider(private val project: Project) : ChangesViewContentProvider {

    private var panel: WorktreeToolWindowPanel? = null

    override fun initTabContent(content: Content) {
        val worktreePanel = WorktreeToolWindowPanel(project)
        panel = worktreePanel
        content.component = worktreePanel
        content.icon = IconLoader.getIcon("/icons/gitWorktree.svg", WorktreeChangesViewContentProvider::class.java)
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, true)
        content.setDisposer(worktreePanel)
    }

    override fun disposeContent() {
        panel = null
    }

    class ContentPreloader(private val project: Project) : ChangesViewContentProvider.Preloader {
        override fun preloadTabContent(content: Content) {
            content.icon = IconLoader.getIcon("/icons/gitWorktree.svg", WorktreeChangesViewContentProvider::class.java)
            content.putUserData(ToolWindow.SHOW_CONTENT_ICON, true)
        }
    }

    class VisibilityPredicate : Predicate<Project> {
        override fun test(project: Project): Boolean {
            return GitRepositoryManager.getInstance(project).repositories.isNotEmpty()
        }
    }
}
