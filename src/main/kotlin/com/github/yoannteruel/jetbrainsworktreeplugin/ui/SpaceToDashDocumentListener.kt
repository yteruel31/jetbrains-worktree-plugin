package com.github.yoannteruel.jetbrainsworktreeplugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField

class SpaceToDashDocumentListener(
    private val project: Project,
    private val field: EditorTextField,
) : DocumentListener {

    private var isUpdating = false

    override fun documentChanged(event: DocumentEvent) {
        if (isUpdating) return
        if (!event.document.text.contains(' ')) return

        isUpdating = true
        ApplicationManager.getApplication().invokeLater {
            val document = event.document
            val text = document.text
            if (text.contains(' ')) {
                val editor = field.editor
                val caretOffset = editor?.caretModel?.offset ?: text.length
                WriteCommandAction.runWriteCommandAction(project) {
                    document.setText(text.replace(' ', '-'))
                }
                editor?.caretModel?.moveToOffset(caretOffset.coerceAtMost(document.textLength))
            }
            isUpdating = false
        }
    }
}
