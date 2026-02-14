package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.services.WorktreeListChangeListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreeSelectionModel

class WorktreeToolWindowPanel(
    private val project: Project,
) : SimpleToolWindowPanel(false, true), Disposable {

    private val rootNode = DefaultMutableTreeNode()
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel).apply {
        cellRenderer = WorktreeTreeCellRenderer()
        isRootVisible = false
        showsRootHandles = true
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        emptyText.text = "No worktrees found"
    }
    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        val actionGroup = ActionManager.getInstance().getAction("GitWorktree.ToolbarActions") as ActionGroup
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("GitWorktreeToolbar", actionGroup, false)
        toolbar.targetComponent = this
        setToolbar(toolbar.component)
        setContent(ScrollPaneFactory.createScrollPane(tree))

        PopupHandler.installPopupMenu(tree, actionGroup, "GitWorktreeTreePopup")

        TreeSpeedSearch.installOn(tree, true) { path ->
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val worktree = node?.userObject as? WorktreeInfo
            worktree?.branchName ?: worktree?.shortHead ?: ""
        }

        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {}
            override fun treeWillCollapse(event: TreeExpansionEvent) {
                val node = event.path.lastPathComponent as? DefaultMutableTreeNode
                val worktree = node?.userObject as? WorktreeInfo
                if (worktree?.isMainWorktree == true) {
                    throw ExpandVetoException(event)
                }
            }
        })

        project.messageBus.connect(this)
            .subscribe(GitWorktreeService.TOPIC, object : WorktreeListChangeListener {
                override fun worktreeListChanged() {
                    refreshWorktrees()
                }
            })

        refreshWorktrees()
    }

    fun getSelectedWorktree(): WorktreeInfo? {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return null
        return node.userObject as? WorktreeInfo
    }

    private fun refreshWorktrees() {
        cs.launch {
            val worktrees = project.service<GitWorktreeService>().listWorktrees()
            withContext(Dispatchers.Main) {
                updateTree(worktrees)
            }
        }
    }

    private fun updateTree(worktrees: List<WorktreeInfo>) {
        val previouslySelected = getSelectedWorktree()

        rootNode.removeAllChildren()

        val mainWorktree = worktrees.firstOrNull { it.isMainWorktree }
        val linkedWorktrees = worktrees.filter { !it.isMainWorktree }

        if (mainWorktree != null) {
            val mainNode = DefaultMutableTreeNode(mainWorktree)
            for (linked in linkedWorktrees) {
                mainNode.add(DefaultMutableTreeNode(linked))
            }
            rootNode.add(mainNode)
        }

        treeModel.reload()

        if (tree.rowCount > 0) {
            tree.expandRow(0)
        }

        if (previouslySelected != null) {
            selectWorktreeByPath(previouslySelected.path)
        }
    }

    private fun selectWorktreeByPath(path: String) {
        for (i in 0 until tree.rowCount) {
            val treePath = tree.getPathForRow(i)
            val node = treePath?.lastPathComponent as? DefaultMutableTreeNode
            val worktree = node?.userObject as? WorktreeInfo
            if (worktree?.path == path) {
                tree.selectionPath = treePath
                return
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
