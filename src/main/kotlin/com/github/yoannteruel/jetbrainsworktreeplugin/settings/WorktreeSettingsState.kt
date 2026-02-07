package com.github.yoannteruel.jetbrainsworktreeplugin.settings

import com.intellij.openapi.components.BaseState

class WorktreeSettingsState : BaseState() {
    var defaultWorktreeDirectory by string()
    var autoSyncIdea by property(false)
    var ideaSyncExclusions by string("workspace.xml, shelf, dataSources, httpRequests, dictionaries")
    var postCreationCommandEnabled by property(false)
    var postCreationCommand by string()
    var showWorktreeInTitle by property(true)
    var openWorktreeInNewWindow by property(true)
}
