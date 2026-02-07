package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class RemoveWorktreeDialog(
    private val project: Project,
    private val worktree: WorktreeInfo,
) : DialogWrapper(project) {

    var closeWindow: Boolean = false
        private set
    var deleteBranch: Boolean = false
        private set

    private val propertyGraph = PropertyGraph()

    private val openProjectForWorktree: Project? = run {
        val normalizedPath = FileUtil.toSystemIndependentName(worktree.path)
        ProjectManager.getInstance().openProjects.find { p ->
            p.basePath?.let { FileUtil.toSystemIndependentName(it) } == normalizedPath
        }
    }
    private val hasOpenWindow = openProjectForWorktree != null
    private val isCurrentProject = openProjectForWorktree === project
    private val hasBranch = !worktree.isDetached && worktree.branchName != null

    private val closeWindowProperty = propertyGraph.property(hasOpenWindow && !isCurrentProject)
    private val deleteBranchProperty = propertyGraph.property(false)

    private lateinit var dialogPanel: DialogPanel

    init {
        title = "Remove Worktree"
        setOKButtonText("Remove")
        init()
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = panel {
            row {
                label("Are you sure you want to remove the worktree at '${worktree.path}'?")
            }

            row {
                checkBox("Close IDE window for this worktree")
                    .bindSelected(closeWindowProperty)
                    .enabled(hasOpenWindow && !isCurrentProject)
                    .applyToComponent {
                        toolTipText = when {
                            isCurrentProject -> "Cannot close the current IDE window"
                            !hasOpenWindow -> "No IDE window is currently open for this worktree"
                            else -> null
                        }
                    }
            }

            row {
                val branchLabel = if (hasBranch) {
                    "Also delete local branch '${worktree.branchName}'"
                } else {
                    "Also delete local branch (detached HEAD — no branch)"
                }
                checkBox(branchLabel)
                    .bindSelected(deleteBranchProperty)
                    .enabled(hasBranch)
                    .applyToComponent {
                        if (!hasBranch) {
                            toolTipText = "This worktree has a detached HEAD — there is no local branch to delete"
                        }
                    }
            }
        }
        return dialogPanel
    }

    override fun doOKAction() {
        dialogPanel.apply()
        closeWindow = closeWindowProperty.get() && hasOpenWindow && !isCurrentProject
        deleteBranch = deleteBranchProperty.get() && hasBranch
        super.doOKAction()
    }
}
