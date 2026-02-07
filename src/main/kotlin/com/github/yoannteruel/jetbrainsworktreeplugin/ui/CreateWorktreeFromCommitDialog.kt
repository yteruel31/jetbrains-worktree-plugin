package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CreateWorktreeFromCommitDialog(
    private val project: Project,
    private val commitHash: String,
) : DialogWrapper(project) {

    var worktreePath: String = ""
        private set
    var createNewBranch: Boolean = false
        private set
    var branchName: String = ""
        private set

    private val propertyGraph = PropertyGraph()
    private val createNewBranchProperty = propertyGraph.property(false)

    private var pathField: String = ""
    private var branchField: String = ""
    private lateinit var dialogPanel: DialogPanel

    init {
        title = "Create Worktree from Commit"
        val settings = project.service<WorktreeSettingsService>()
        val defaultDir = settings.state.defaultWorktreeDirectory ?: ""
        pathField = if (defaultDir.isNotBlank()) {
            "$defaultDir/${commitHash.take(7)}"
        } else {
            ""
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = panel {
            row("Commit:") {
                textField()
                    .columns(40)
                    .apply { component.text = commitHash.take(12); component.isEditable = false }
                    .align(AlignX.FILL)
            }

            row("Worktree path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Worktree Location"),
                    project,
                ).columns(40)
                    .bindText(::pathField)
                    .align(AlignX.FILL)
            }

            row {
                checkBox("Create new branch at this commit")
                    .bindSelected(createNewBranchProperty)
            }

            row("Branch name:") {
                textField()
                    .columns(30)
                    .bindText(::branchField)
                    .align(AlignX.FILL)
            }.visibleIf(createNewBranchProperty)
        }
        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        dialogPanel.apply()
        if (pathField.isBlank()) {
            return ValidationInfo("Worktree path must not be empty")
        }
        if (createNewBranchProperty.get() && branchField.isBlank()) {
            return ValidationInfo("Branch name must not be empty when creating a new branch")
        }
        return null
    }

    override fun doOKAction() {
        dialogPanel.apply()
        worktreePath = pathField
        createNewBranch = createNewBranchProperty.get()
        branchName = branchField
        super.doOKAction()
    }
}
