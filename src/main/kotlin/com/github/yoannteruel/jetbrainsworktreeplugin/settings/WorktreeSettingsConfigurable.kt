package com.github.yoannteruel.jetbrainsworktreeplugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.CheckBoxList
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.openapi.ui.Messages
import javax.swing.JComponent

class WorktreeSettingsConfigurable(private val project: Project) : Configurable {

    private val settings get() = WorktreeSettingsService.getInstance(project)
    private var panel: com.intellij.openapi.ui.DialogPanel? = null
    private var checkBoxList: CheckBoxList<String>? = null

    override fun getDisplayName(): String = "Git Worktree Tool"

    private val propertyGraph = PropertyGraph()

    override fun createComponent(): JComponent {
        val state = settings.state
        val hookEnabledProperty = propertyGraph.property(state.postCreationCommandEnabled)

        val cbList = CheckBoxList<String>()
        checkBoxList = cbList
        loadCustomEntries(cbList)

        val decorator = ToolbarDecorator.createDecorator(cbList)
            .setAddAction {
                val path = Messages.showInputDialog(
                    project,
                    "Enter path relative to project root (e.g., .claude/, .env):",
                    "Add Sync Path",
                    null
                )
                if (!path.isNullOrBlank()) {
                    val trimmed = path.trim()
                    if (trimmed.startsWith("/") || trimmed.contains("..")) {
                        Messages.showErrorDialog(
                            project,
                            "Path must be relative and cannot contain '..'.",
                            "Invalid Path"
                        )
                        return@setAddAction
                    }
                    // Check for duplicates
                    for (i in 0 until cbList.model.size) {
                        if (cbList.getItemAt(i) == trimmed) {
                            Messages.showErrorDialog(project, "Path '$trimmed' is already in the list.", "Duplicate Path")
                            return@setAddAction
                        }
                    }
                    cbList.addItem(trimmed, trimmed, true)
                }
            }
            .setRemoveAction {
                val idx = cbList.selectedIndex
                if (idx >= 0) {
                    @Suppress("UNCHECKED_CAST")
                    (cbList.model as javax.swing.DefaultListModel<Any>).remove(idx)
                }
            }
            .disableUpDownActions()

        panel = panel {
            group("General") {
                row("Default worktree directory:") {
                    textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle("Select Default Worktree Directory"),
                        project
                    ).columns(COLUMNS_LARGE)
                        .bindText(
                            getter = { state.defaultWorktreeDirectory ?: "" },
                            setter = { state.defaultWorktreeDirectory = it.ifBlank { null } }
                        )
                }
                row {
                    checkBox("Auto-sync on worktree creation")
                        .bindSelected(state::autoSync)
                }
                row {
                    checkBox("Show worktree branch in window title")
                        .bindSelected(state::showWorktreeInTitle)
                }
                row {
                    checkBox("Open worktree in new window after creation")
                        .bindSelected(state::openAfterCreation)
                }
            }
            group("Sync Configuration") {
                row {
                    comment("Paths synced from main worktree to linked worktrees.")
                }
                row {
                    checkBox(".idea/")
                        .bindSelected(state::ideaSyncEnabled)
                    link("Configure exclusions...") {
                        showIdeaExclusionsDialog()
                    }
                }
                separator()
                row {
                    cell(decorator.createPanel())
                        .align(AlignX.FILL)
                        .onApply { saveCustomEntries() }
                        .onReset { loadCustomEntries(cbList) }
                        .onIsModified { isCustomEntriesModified() }
                }
            }
            group("Post-Creation Hook") {
                row {
                    checkBox("Run command after worktree creation")
                        .bindSelected(hookEnabledProperty)
                        .onChanged { state.postCreationCommandEnabled = it.isSelected }
                }
                row("Command:") {
                    textField()
                        .columns(COLUMNS_LARGE)
                        .bindText(
                            getter = { state.postCreationCommand ?: "" },
                            setter = { state.postCreationCommand = it.ifBlank { null } }
                        )
                }.visibleIf(hookEnabledProperty)
                row {
                    comment("Command runs in the new worktree directory (e.g. <code>npm install</code>, <code>mvn clean install</code>)")
                }.visibleIf(hookEnabledProperty)
            }
        }
        return panel!!
    }

    private fun showIdeaExclusionsDialog() {
        val current = settings.state.ideaSyncExclusions
            ?.split(",")
            ?.joinToString("\n") { it.trim() }
            ?: ""
        val result = Messages.showMultilineInputDialog(
            project,
            "File and directory names to exclude from .idea sync (one per line):",
            ".idea Sync Exclusions",
            current,
            null,
            null
        )
        if (result != null) {
            settings.state.ideaSyncExclusions = result.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
        }
    }

    private fun loadCustomEntries(cbList: CheckBoxList<String>) {
        cbList.clear()
        for (entry in settings.state.customSyncEntries) {
            val path = entry.path ?: continue
            cbList.addItem(path, path, entry.enabled)
        }
    }

    private fun saveCustomEntries() {
        val cbList = checkBoxList ?: return
        val state = settings.state
        state.customSyncEntries.clear()
        for (i in 0 until cbList.model.size) {
            val path = cbList.getItemAt(i) ?: continue
            val entry = SyncEntryState()
            entry.path = path
            entry.enabled = cbList.isItemSelected(i)
            state.customSyncEntries.add(entry)
        }
    }

    private fun isCustomEntriesModified(): Boolean {
        val cbList = checkBoxList ?: return false
        val entries = settings.state.customSyncEntries
        if (cbList.model.size != entries.size) return true
        for (i in 0 until cbList.model.size) {
            val path = cbList.getItemAt(i) ?: return true
            val entry = entries[i]
            if (path != entry.path) return true
            if (cbList.isItemSelected(i) != entry.enabled) return true
        }
        return false
    }

    override fun isModified(): Boolean = panel?.isModified() == true

    override fun apply() {
        panel?.apply()
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
        checkBoxList = null
    }
}
