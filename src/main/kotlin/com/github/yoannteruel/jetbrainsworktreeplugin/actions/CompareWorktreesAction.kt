package com.github.yoannteruel.jetbrainsworktreeplugin.actions

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.table.JBTable
import javax.swing.table.AbstractTableModel

class CompareWorktreesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktrees = GitWorktreeService.getInstance(project).listWorktrees()
        if (worktrees.isEmpty()) return

        val table = JBTable(WorktreeCompareTableModel(worktrees))
        table.setShowGrid(true)

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(table, null)
            .setTitle("Compare Worktrees")
            .setResizable(true)
            .setMovable(true)
            .createPopup()
            .showInBestPositionFor(e.dataContext)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT

    private class WorktreeCompareTableModel(
        private val worktrees: List<WorktreeInfo>
    ) : AbstractTableModel() {

        private val columns = arrayOf("Path", "Branch", "Commit", "Status")

        override fun getRowCount(): Int = worktrees.size

        override fun getColumnCount(): Int = columns.size

        override fun getColumnName(column: Int): String = columns[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val wt = worktrees[rowIndex]
            return when (columnIndex) {
                0 -> wt.path
                1 -> wt.branchName ?: "(detached)"
                2 -> wt.shortHead
                3 -> buildStatus(wt)
                else -> ""
            }
        }

        private fun buildStatus(wt: WorktreeInfo): String {
            val parts = mutableListOf<String>()
            if (wt.isMainWorktree) parts.add("main")
            if (wt.isLocked) parts.add("locked")
            if (wt.isBare) parts.add("bare")
            if (wt.isDetached) parts.add("detached")
            return parts.joinToString(", ").ifEmpty { "active" }
        }
    }
}
