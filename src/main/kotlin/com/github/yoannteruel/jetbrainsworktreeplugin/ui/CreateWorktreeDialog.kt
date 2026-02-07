package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CreateWorktreeDialog(private val project: Project) : DialogWrapper(project) {

    var worktreePath: String = ""
        private set
    var branchName: String = ""
        private set
    var createNewBranch: Boolean = true
        private set
    var baseBranch: String? = null
        private set

    private val propertyGraph = PropertyGraph()
    private val createNewBranchProperty = propertyGraph.property(true)

    private var availableBranches: List<String> = emptyList()
    private var selectedExistingBranch: String = ""
    private var pathField: String = ""

    init {
        title = "Create Worktree"
        availableBranches = project.service<GitWorktreeService>().getAvailableBranches()
        if (availableBranches.isNotEmpty()) {
            selectedExistingBranch = availableBranches.first()
            baseBranch = availableBranches.first()
        }

        val settings = project.service<WorktreeSettingsService>()
        pathField = settings.state.defaultWorktreeDirectory ?: ""

        init()
    }

    override fun createCenterPanel(): JComponent = panel {
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
            checkBox("Create new branch")
                .bindSelected(createNewBranchProperty)
        }

        row("Branch name:") {
            textField()
                .columns(30)
                .bindText(::branchName)
                .align(AlignX.FILL)
        }.visibleIf(createNewBranchProperty)

        row("Base branch:") {
            comboBox(availableBranches)
                .align(AlignX.FILL)
                .onChanged { baseBranch = it.item as? String }
        }.visibleIf(createNewBranchProperty)

        row("Existing branch:") {
            comboBox(availableBranches)
                .align(AlignX.FILL)
                .onChanged { selectedExistingBranch = it.item as? String ?: "" }
        }.visibleIf(createNewBranchProperty.not())
    }

    override fun doValidate(): ValidationInfo? {
        if (pathField.isBlank()) {
            return ValidationInfo("Worktree path must not be empty")
        }
        if (createNewBranchProperty.get() && branchName.isBlank()) {
            return ValidationInfo("Branch name must not be empty when creating a new branch")
        }
        if (!createNewBranchProperty.get() && selectedExistingBranch.isBlank()) {
            return ValidationInfo("Please select a branch")
        }
        return null
    }

    override fun doOKAction() {
        createNewBranch = createNewBranchProperty.get()
        worktreePath = pathField
        if (!createNewBranch) {
            branchName = selectedExistingBranch
            baseBranch = null
        }
        super.doOKAction()
    }
}
