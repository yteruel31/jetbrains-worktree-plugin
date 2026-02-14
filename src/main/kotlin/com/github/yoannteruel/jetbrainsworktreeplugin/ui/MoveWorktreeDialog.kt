package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.AlignX
import java.io.File
import javax.swing.JComponent

class MoveWorktreeDialog(
    private val project: Project,
    private val currentPath: String,
) : DialogWrapper(project) {

    var newPath: String = ""
        private set

    private lateinit var newPathComponent: TextFieldWithBrowseButton

    init {
        title = "Move Worktree"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Current path:") {
            textField()
                .columns(40)
                .align(AlignX.FILL)
                .applyToComponent {
                    text = currentPath
                    isEditable = false
                }
        }

        row("New path:") {
            textFieldWithBrowseButton(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle("Select New Location"),
                project,
            ).columns(40)
                .align(AlignX.FILL)
                .applyToComponent { newPathComponent = this }
        }
    }

    override fun doValidate(): ValidationInfo? {
        val path = newPathComponent.text
        if (path.isBlank()) {
            return ValidationInfo("New path must not be empty")
        }
        if (path == currentPath) {
            return ValidationInfo("New path must be different from the current path")
        }
        if (File(path).exists()) {
            return ValidationInfo("The destination path already exists")
        }
        return null
    }

    override fun doOKAction() {
        newPath = newPathComponent.text
        super.doOKAction()
    }
}
