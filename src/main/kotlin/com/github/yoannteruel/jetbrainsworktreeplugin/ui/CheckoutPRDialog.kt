package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.github.yoannteruel.jetbrainsworktreeplugin.services.GitWorktreeService
import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CheckoutPRDialog(
    private val project: Project,
    private val provider: GitWorktreeService.RemoteProvider,
) : DialogWrapper(project) {

    var prNumber: Int = 0
        private set
    var worktreePath: String = ""
        private set

    private var prNumberField: String = ""
    private var pathField: String = ""
    private lateinit var dialogPanel: DialogPanel

    init {
        title = "Checkout ${providerLabel()} Request"
        val settings = project.service<WorktreeSettingsService>()
        pathField = settings.state.defaultWorktreeDirectory ?: ""
        init()
    }

    private fun providerLabel(): String = when (provider) {
        GitWorktreeService.RemoteProvider.GITHUB -> "Pull"
        GitWorktreeService.RemoteProvider.GITLAB -> "Merge"
        GitWorktreeService.RemoteProvider.BITBUCKET -> "Pull"
    }

    private fun numberLabel(): String = when (provider) {
        GitWorktreeService.RemoteProvider.GITHUB -> "PR number:"
        GitWorktreeService.RemoteProvider.GITLAB -> "MR number:"
        GitWorktreeService.RemoteProvider.BITBUCKET -> "PR number:"
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = panel {
            row("Provider:") {
                label(provider.label)
            }

            row(numberLabel()) {
                textField()
                    .columns(15)
                    .bindText(::prNumberField)
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
        }
        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        dialogPanel.apply()
        val number = prNumberField.trim().toIntOrNull()
        if (number == null || number <= 0) {
            return ValidationInfo("Please enter a valid ${providerLabel().lowercase()} request number")
        }
        if (pathField.isBlank()) {
            return ValidationInfo("Worktree path must not be empty")
        }
        return null
    }

    override fun doOKAction() {
        dialogPanel.apply()
        prNumber = prNumberField.trim().toInt()
        worktreePath = pathField
        super.doOKAction()
    }
}
