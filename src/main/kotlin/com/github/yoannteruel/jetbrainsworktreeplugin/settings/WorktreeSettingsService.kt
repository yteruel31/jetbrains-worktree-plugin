package com.github.yoannteruel.jetbrainsworktreeplugin.settings

import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@State(name = "GitWorktreeManagerSettings", storages = [Storage("gitWorktreeManager.xml")])
class WorktreeSettingsService :
    SimplePersistentStateComponent<WorktreeSettingsState>(WorktreeSettingsState()) {

    companion object {
        fun getInstance(project: Project): WorktreeSettingsService = project.service()
    }
}
