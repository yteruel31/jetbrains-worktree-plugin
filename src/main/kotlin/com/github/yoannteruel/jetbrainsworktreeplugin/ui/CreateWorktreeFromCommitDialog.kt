package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CreateWorktreeFromCommitDialog(
    private val project: Project,
    private val commitHash: String,
    private val availableBranches: List<String>,
) : DialogWrapper(project) {

    var worktreePath: String = ""
        private set
    var createNewBranch: Boolean = false
        private set
    var branchName: String = ""
        private set
    var syncFiles: Boolean = false
        private set
    var runPostCreationCommand: Boolean = false
        private set
    var openInNewWindow: Boolean = false
        private set

    private val settings = project.service<WorktreeSettingsService>()
    private val propertyGraph = PropertyGraph()
    private val createNewBranchProperty = propertyGraph.property(false)
    private val syncFilesProperty = propertyGraph.property(false)
    private val runPostCreationCommandProperty = propertyGraph.property(false)
    private val openInNewWindowProperty = propertyGraph.property(false)

    private var pathField: String = ""
    private lateinit var dialogPanel: DialogPanel
    private lateinit var branchNameField: TextFieldWithAutoCompletion<String>

    init {
        title = "Create Worktree from Commit"
        val defaultDir = settings.state.defaultWorktreeDirectory ?: ""
        pathField = if (defaultDir.isNotBlank()) {
            "$defaultDir/${commitHash.take(7)}"
        } else {
            ""
        }

        syncFiles = settings.state.autoSync
        runPostCreationCommand = !settings.state.postCreationCommand.isNullOrBlank()
                && settings.state.postCreationCommandEnabled
        openInNewWindow = settings.state.openAfterCreation

        syncFilesProperty.set(syncFiles)
        runPostCreationCommandProperty.set(runPostCreationCommand)
        openInNewWindowProperty.set(openInNewWindow)

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
                branchNameField = TextFieldWithAutoCompletion.create(project, availableBranches, false, "")
                branchNameField.setPlaceholder("Type or select a branch name")
                branchNameField.document.addDocumentListener(SpaceToDashDocumentListener(project, branchNameField))
                cell(branchNameField)
                    .align(AlignX.FILL)
            }.visibleIf(createNewBranchProperty)

            separator()

            row {
                checkBox("Sync files to new worktree")
                    .bindSelected(syncFilesProperty)
            }

            if (!settings.state.postCreationCommand.isNullOrBlank()) {
                row {
                    checkBox("Run post-creation command")
                        .bindSelected(runPostCreationCommandProperty)
                }
                row {
                    comment(settings.state.postCreationCommand!!)
                }.visibleIf(runPostCreationCommandProperty)
            }

            row {
                checkBox("Open in new window")
                    .bindSelected(openInNewWindowProperty)
            }
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
        return null
    }

    override fun doOKAction() {
        dialogPanel.apply()
        worktreePath = pathField
        createNewBranch = createNewBranchProperty.get()
        syncFiles = syncFilesProperty.get()
        runPostCreationCommand = runPostCreationCommandProperty.get()
        openInNewWindow = openInNewWindowProperty.get()
        if (createNewBranch) {
            branchName = branchNameField.text
        }
        super.doOKAction()
    }
}
