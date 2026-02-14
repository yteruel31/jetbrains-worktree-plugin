package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.openapi.ui.DialogPanel
import javax.swing.JComponent

class CreateWorktreeDialog(
    private val project: Project,
    private val availableBranches: List<String>,
    private val allBranches: List<String>,
) : DialogWrapper(project) {

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

    private var selectedExistingBranch: String = ""
    private var pathField: String = ""
    private lateinit var dialogPanel: DialogPanel
    private lateinit var branchNameField: TextFieldWithAutoCompletion<String>

    init {
        title = "Create Worktree"
        if (availableBranches.isNotEmpty()) {
            selectedExistingBranch = availableBranches.first()
        }
        if (allBranches.isNotEmpty()) {
            baseBranch = allBranches.first()
        }

        val settings = project.service<WorktreeSettingsService>()
        pathField = settings.state.defaultWorktreeDirectory ?: ""

        init()
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = panel {
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
            branchNameField = TextFieldWithAutoCompletion.create(project, availableBranches, false, "")
            branchNameField.setPlaceholder("Type or select a branch name")
            branchNameField.document.addDocumentListener(SpaceToDashDocumentListener(project, branchNameField))
            cell(branchNameField)
                .align(AlignX.FILL)
        }.visibleIf(createNewBranchProperty)

        row("Base branch:") {
            comboBox(allBranches)
                .align(AlignX.FILL)
                .onChanged { baseBranch = it.item as? String }
        }.visibleIf(createNewBranchProperty)

        row("Existing branch:") {
            comboBox(availableBranches)
                .align(AlignX.FILL)
                .onChanged { selectedExistingBranch = it.item as? String ?: "" }
        }.visibleIf(createNewBranchProperty.not())
        }
        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        dialogPanel.apply()
        if (pathField.isBlank()) {
            return ValidationInfo("Worktree path must not be empty")
        }
        if (createNewBranchProperty.get() && branchNameField.text.isBlank()) {
            return ValidationInfo("Branch name must not be empty when creating a new branch")
        }
        if (!createNewBranchProperty.get() && selectedExistingBranch.isBlank()) {
            return ValidationInfo("Please select a branch")
        }
        return null
    }

    override fun doOKAction() {
        dialogPanel.apply()
        createNewBranch = createNewBranchProperty.get()
        worktreePath = pathField
        if (createNewBranch) {
            branchName = branchNameField.text
        } else {
            branchName = selectedExistingBranch
            baseBranch = null
        }
        super.doOKAction()
    }
}
