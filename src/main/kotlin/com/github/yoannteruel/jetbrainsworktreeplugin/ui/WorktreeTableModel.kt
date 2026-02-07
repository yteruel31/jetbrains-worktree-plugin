package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.io.File
import javax.swing.table.TableCellRenderer
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.JTable

class WorktreeTableModel : ListTableModel<WorktreeInfo>(
    PathColumnInfo(),
    BranchColumnInfo(),
    CommitColumnInfo(),
    StatusColumnInfo(),
)

private class PathColumnInfo : ColumnInfo<WorktreeInfo, String>("Path") {
    override fun valueOf(item: WorktreeInfo): String = File(item.path).name

    override fun getTooltipText(): String = "Full worktree path"

    override fun getRenderer(item: WorktreeInfo?): TableCellRenderer {
        return object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int,
            ): java.awt.Component {
                val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                if (item != null) {
                    toolTipText = item.path
                }
                return component
            }
        }
    }
}

private class BranchColumnInfo : ColumnInfo<WorktreeInfo, String>("Branch") {
    override fun valueOf(item: WorktreeInfo): String = item.branchName ?: "(detached)"
}

private class CommitColumnInfo : ColumnInfo<WorktreeInfo, String>("Commit") {
    override fun valueOf(item: WorktreeInfo): String = item.shortHead
}

private class StatusColumnInfo : ColumnInfo<WorktreeInfo, String>("Status") {
    override fun valueOf(item: WorktreeInfo): String {
        val parts = mutableListOf<String>()
        if (item.isMainWorktree) parts.add("main")
        if (item.isLocked) parts.add("locked")
        if (item.isBare) parts.add("bare")
        return parts.joinToString(", ")
    }
}
