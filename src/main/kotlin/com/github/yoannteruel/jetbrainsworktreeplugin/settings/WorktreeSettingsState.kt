package com.github.yoannteruel.jetbrainsworktreeplugin.settings

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.XCollection

class SyncEntryState : BaseState() {
    var path by string("")
    var enabled by property(true)
}

class WorktreeSettingsState : BaseState() {
    var defaultWorktreeDirectory by string()
    var autoSync by property(false)
    var ideaSyncEnabled by property(true)
    var ideaSyncExclusions by string("workspace.xml, shelf, dataSources, httpRequests, dictionaries")
    @get:XCollection(elementName = "entry")
    var customSyncEntries by list<SyncEntryState>()
    var autoSyncIdea by property(false)
    var settingsMigrated by property(false)
    var postCreationCommandEnabled by property(false)
    var postCreationCommand by string()
    var showWorktreeInTitle by property(true)
    var openAfterCreation by property(false)
}
