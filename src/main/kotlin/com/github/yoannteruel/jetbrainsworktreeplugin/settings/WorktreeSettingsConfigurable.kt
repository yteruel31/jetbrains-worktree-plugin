package com.github.yoannteruel.jetbrainsworktreeplugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import javax.swing.JComponent

class WorktreeSettingsConfigurable(private val project: Project) : Configurable {

    private val settings get() = WorktreeSettingsService.getInstance(project)
    private var panel: com.intellij.openapi.ui.DialogPanel? = null

    override fun getDisplayName(): String = "Git Worktree Manager"

    private val propertyGraph = PropertyGraph()

    override fun createComponent(): JComponent {
        val state = settings.state
        val hookEnabledProperty = propertyGraph.property(state.postCreationCommandEnabled)
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
                    checkBox("Auto-sync .idea on worktree creation")
                        .bindSelected(state::autoSyncIdea)
                }
                row {
                    checkBox("Show worktree branch in window title")
                        .bindSelected(state::showWorktreeInTitle)
                }
                row {
                    checkBox("Open worktrees in new window")
                        .bindSelected(state::openWorktreeInNewWindow)
                }
            }
            group(".idea Sync Exclusions") {
                row {
                    comment("One pattern per line (file or directory names to exclude from .idea sync)")
                }
                row {
                    textArea()
                        .columns(COLUMNS_LARGE)
                        .rows(6)
                        .bindText(
                            getter = {
                                state.ideaSyncExclusions
                                    ?.split(",")
                                    ?.joinToString("\n") { it.trim() }
                                    ?: ""
                            },
                            setter = {
                                state.ideaSyncExclusions = it.lines()
                                    .map { line -> line.trim() }
                                    .filter { line -> line.isNotEmpty() }
                                    .joinToString(", ")
                            }
                        )
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

    override fun isModified(): Boolean = panel?.isModified() == true

    override fun apply() {
        panel?.apply()
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}
