package com.github.yoannteruel.jetbrainsworktreeplugin.model

data class WorktreeInfo(
    val path: String,
    val head: String,
    val branch: String?,
    val isMainWorktree: Boolean,
    val isBare: Boolean,
    val isDetached: Boolean,
    val isLocked: Boolean,
    val lockReason: String?,
) {
    val branchName: String?
        get() = branch?.removePrefix("refs/heads/")

    val shortHead: String
        get() = head.take(7)
}
