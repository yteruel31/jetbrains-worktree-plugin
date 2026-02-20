package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.ui.WorktreeToolWindowPanel
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer
import com.intellij.openapi.vcs.changes.ui.ChangeDiffRequestChain
import com.intellij.ui.SimpleListCellRenderer
import git4idea.changes.GitChangeUtils
import git4idea.repo.GitRepositoryManager

class CompareWorktreesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dataContext = e.dataContext
        val selectedWorktree = e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Loading worktrees...", false
        ) {
            override fun run(indicator: ProgressIndicator) {
                val allWorktrees = GitWorktreeService.getInstance(project).listWorktrees()
                val otherWorktrees = allWorktrees.filter { it.path != selectedWorktree.path }

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) return@invokeLater
                    if (otherWorktrees.isEmpty()) {
                        Messages.showInfoMessage(project, "No other worktrees to compare with.", "Compare Worktrees")
                        return@invokeLater
                    }

                    if (otherWorktrees.size == 1) {
                        showDiff(project, selectedWorktree, otherWorktrees.first())
                        return@invokeLater
                    }

                    JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(otherWorktrees)
                        .setTitle("Compare '${selectedWorktree.branchName ?: selectedWorktree.shortHead}' with")
                        .setRenderer(SimpleListCellRenderer.create { label, value, _ ->
                            label.text = value.branchName ?: "(detached) ${value.shortHead}"
                        })
                        .setItemChosenCallback { target ->
                            showDiff(project, selectedWorktree, target)
                        }
                        .createPopup()
                        .showInBestPositionFor(dataContext)
                }
            }
        })
    }

    private fun showDiff(project: Project, source: WorktreeInfo, target: WorktreeInfo) {
        val repo = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return
        val sourceRef = source.branchName ?: source.head
        val targetRef = target.branchName ?: target.head

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Comparing worktrees...", true
        ) {
            override fun run(indicator: ProgressIndicator) {
                val changes = GitChangeUtils.getDiff(repo, sourceRef, targetRef, false) ?: emptyList()
                val producers = changes.mapNotNull { ChangeDiffRequestProducer.create(project, it) }

                if (producers.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(project, "No differences found.", "Compare Worktrees")
                    }
                    return
                }

                val chain = ChangeDiffRequestChain(producers, 0)
                ApplicationManager.getApplication().invokeLater {
                    DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.FRAME)
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
                && e.getData(WorktreeToolWindowPanel.SELECTED_WORKTREE) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
