package com.github.yoannteruel.jetbrainsworktreeplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class WorktreeCacheStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        GitWorktreeService.getInstance(project).refreshCacheAsync()
    }
}
