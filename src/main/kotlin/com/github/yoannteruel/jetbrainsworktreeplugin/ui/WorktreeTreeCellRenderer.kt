package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class WorktreeTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ) {
        val node = value as? DefaultMutableTreeNode ?: return
        val worktree = node.userObject as? WorktreeInfo ?: return

        icon = when {
            worktree.isMainWorktree -> AllIcons.Nodes.Favorite
            worktree.isLocked -> AllIcons.Diff.Lock
            else -> AllIcons.Vcs.Branch
        }

        val displayName = worktree.branchName ?: "(detached)"
        append(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        append("  ${worktree.shortHead}", SimpleTextAttributes.GRAYED_ATTRIBUTES)

        toolTipText = worktree.path
    }
}
