package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.WorktreeListChangeListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorktreeToolWindowPanel(
    private val project: Project,
) : SimpleToolWindowPanel(true, true), Disposable {

    private val tableModel = WorktreeTableModel()
    private val table = JBTable(tableModel).apply {
        setShowGrid(false)
        emptyText.text = "No worktrees found"
    }
    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        val actionGroup = ActionManager.getInstance().getAction("GitWorktree.ToolbarActions")
            as com.intellij.openapi.actionSystem.ActionGroup
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("GitWorktreeToolbar", actionGroup, false)
        toolbar.targetComponent = this
        setToolbar(toolbar.component)
        setContent(ScrollPaneFactory.createScrollPane(table))

        project.messageBus.connect(this)
            .subscribe(GitWorktreeService.TOPIC, object : WorktreeListChangeListener {
                override fun worktreeListChanged() {
                    refreshWorktrees()
                }
            })

        refreshWorktrees()
    }

    fun getSelectedWorktree(): WorktreeInfo? {
        val row = table.selectedRow
        if (row < 0) return null
        return tableModel.getItem(row)
    }

    private fun refreshWorktrees() {
        cs.launch {
            val worktrees = project.service<GitWorktreeService>().listWorktrees()
            withContext(Dispatchers.Main) {
                tableModel.items = worktrees
            }
        }
    }

    override fun uiDataSnapshot(sink: DataSink) {
        super.uiDataSnapshot(sink)
        sink.lazy(SELECTED_WORKTREE) {
            getSelectedWorktree()
        }
    }

    override fun dispose() {
        cs.cancel()
    }

    companion object {
        val SELECTED_WORKTREE = DataKey.create<WorktreeInfo>("GitWorktree.SelectedWorktree")
    }
}
