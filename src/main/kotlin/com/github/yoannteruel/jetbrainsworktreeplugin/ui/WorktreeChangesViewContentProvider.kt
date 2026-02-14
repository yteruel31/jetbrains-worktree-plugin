package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider
import com.intellij.ui.content.Content
import git4idea.repo.GitRepositoryManager
import java.util.function.Predicate

class WorktreeChangesViewContentProvider(private val project: Project) : ChangesViewContentProvider {

    private var panel: WorktreeToolWindowPanel? = null

    override fun initTabContent(content: Content) {
        val worktreePanel = WorktreeToolWindowPanel(project)
        panel = worktreePanel
        content.component = worktreePanel
        content.setDisposer(worktreePanel)
    }

    override fun disposeContent() {
        panel = null
    }

    class VisibilityPredicate : Predicate<Project> {
        override fun test(project: Project): Boolean {
            return GitRepositoryManager.getInstance(project).repositories.isNotEmpty()
        }
    }
}
