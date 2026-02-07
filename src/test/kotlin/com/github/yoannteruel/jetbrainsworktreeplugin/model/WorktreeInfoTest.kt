package com.github.yoannteruel.jetbrainsworktreeplugin.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorktreeInfoTest {

    @Test
    fun `branchName strips refs-heads prefix`() {
        val info = worktree(branch = "refs/heads/main")
        assertEquals("main", info.branchName)
    }

    @Test
    fun `branchName handles nested branch names`() {
        val info = worktree(branch = "refs/heads/feature/my-feature")
        assertEquals("feature/my-feature", info.branchName)
    }

    @Test
    fun `branchName returns null when branch is null`() {
        val info = worktree(branch = null)
        assertNull(info.branchName)
    }

    @Test
    fun `branchName returns string as-is when no refs-heads prefix`() {
        val info = worktree(branch = "main")
        assertEquals("main", info.branchName)
    }

    @Test
    fun `shortHead returns first 7 characters`() {
        val info = worktree(head = "abc123def456789")
        assertEquals("abc123d", info.shortHead)
    }

    @Test
    fun `shortHead handles exactly 7 characters`() {
        val info = worktree(head = "abc1234")
        assertEquals("abc1234", info.shortHead)
    }

    @Test
    fun `shortHead handles shorter hash gracefully`() {
        val info = worktree(head = "abc")
        assertEquals("abc", info.shortHead)
    }

    private fun worktree(
        path: String = "/test",
        head: String = "abc123def456789",
        branch: String? = "refs/heads/main",
        isMainWorktree: Boolean = false,
        isBare: Boolean = false,
        isDetached: Boolean = false,
        isLocked: Boolean = false,
        lockReason: String? = null,
    ) = WorktreeInfo(path, head, branch, isMainWorktree, isBare, isDetached, isLocked, lockReason)
}
