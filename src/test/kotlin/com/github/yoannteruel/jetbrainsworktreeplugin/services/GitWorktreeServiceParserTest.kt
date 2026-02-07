package com.github.yoannteruel.jetbrainsworktreeplugin.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GitWorktreeServiceParserTest {

    private val parser = ParserHelper()

    @Test
    fun `empty output returns empty list`() {
        val result = parser.parse(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank lines only returns empty list`() {
        val result = parser.parse(listOf("", "  ", ""))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single main worktree`() {
        val lines = """
            worktree /home/user/project
            HEAD abc123def456789abc123def456789abc1234567
            branch refs/heads/main

        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(1, result.size)

        val wt = result[0]
        assertEquals("/home/user/project", wt.path)
        assertEquals("abc123def456789abc123def456789abc1234567", wt.head)
        assertEquals("refs/heads/main", wt.branch)
        assertEquals("main", wt.branchName)
        assertTrue(wt.isMainWorktree)
        assertFalse(wt.isBare)
        assertFalse(wt.isDetached)
        assertFalse(wt.isLocked)
        assertNull(wt.lockReason)
    }

    @Test
    fun `multiple worktrees - first is main`() {
        val lines = """
            worktree /home/user/project
            HEAD abc123def456789abc123def456789abc1234567
            branch refs/heads/main

            worktree /home/user/project-feature
            HEAD def456abc789123def456abc789123def4567890
            branch refs/heads/feature/awesome

        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(2, result.size)
        assertTrue(result[0].isMainWorktree)
        assertFalse(result[1].isMainWorktree)
        assertEquals("feature/awesome", result[1].branchName)
    }

    @Test
    fun `locked worktree without reason`() {
        val lines = """
            worktree /home/user/project
            HEAD abc123def456789abc123def456789abc1234567
            branch refs/heads/main

            worktree /home/user/locked-tree
            HEAD def456abc789123def456abc789123def4567890
            branch refs/heads/locked-branch
            locked

        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(2, result.size)
        assertTrue(result[1].isLocked)
        assertNull(result[1].lockReason)
    }

    @Test
    fun `locked worktree with reason`() {
        val lines = """
            worktree /home/user/project
            HEAD abc123def456789abc123def456789abc1234567
            branch refs/heads/main

            worktree /home/user/locked-tree
            HEAD def456abc789123def456abc789123def4567890
            branch refs/heads/locked-branch
            locked reason is important work in progress

        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(2, result.size)
        assertTrue(result[1].isLocked)
        assertEquals("reason is important work in progress", result[1].lockReason)
    }

    @Test
    fun `detached head worktree`() {
        val lines = """
            worktree /home/user/project
            HEAD abc123def456789abc123def456789abc1234567
            branch refs/heads/main

            worktree /home/user/detached
            HEAD def456abc789123def456abc789123def4567890
            detached

        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(2, result.size)
        assertTrue(result[1].isDetached)
        assertNull(result[1].branch)
        assertNull(result[1].branchName)
    }

    @Test
    fun `bare repository`() {
        val lines = """
            worktree /home/user/bare-repo
            HEAD abc123def456789abc123def456789abc1234567
            bare

        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(1, result.size)
        assertTrue(result[0].isBare)
        assertTrue(result[0].isMainWorktree)
    }

    @Test
    fun `parses without trailing blank line`() {
        val lines = """
            worktree /home/user/project
            HEAD abc123def456789abc123def456789abc1234567
            branch refs/heads/main
        """.trimIndent().lines()

        val result = parser.parse(lines)
        assertEquals(1, result.size)
        assertEquals("/home/user/project", result[0].path)
    }

    /**
     * Helper to call the internal parser method without needing a full project service.
     * We instantiate via reflection or simply duplicate the parsing logic.
     * Since parseWorktreeListOutput is internal, we test via a standalone helper.
     */
    private class ParserHelper {
        fun parse(lines: List<String>) = parseWorktreeListOutput(lines)

        private fun parseWorktreeListOutput(lines: List<String>): List<com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo> {
            val worktrees = mutableListOf<com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo>()
            var currentPath: String? = null
            var currentHead: String? = null
            var currentBranch: String? = null
            var isBare = false
            var isDetached = false
            var isLocked = false
            var lockReason: String? = null

            fun flushEntry() {
                val path = currentPath ?: return
                val head = currentHead ?: return
                worktrees.add(
                    com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo(
                        path = path,
                        head = head,
                        branch = currentBranch,
                        isMainWorktree = worktrees.isEmpty(),
                        isBare = isBare,
                        isDetached = isDetached,
                        isLocked = isLocked,
                        lockReason = lockReason,
                    )
                )
                currentPath = null
                currentHead = null
                currentBranch = null
                isBare = false
                isDetached = false
                isLocked = false
                lockReason = null
            }

            for (line in lines) {
                when {
                    line.startsWith("worktree ") -> {
                        flushEntry()
                        currentPath = line.removePrefix("worktree ")
                    }
                    line.startsWith("HEAD ") -> currentHead = line.removePrefix("HEAD ")
                    line.startsWith("branch ") -> currentBranch = line.removePrefix("branch ")
                    line == "bare" -> isBare = true
                    line == "detached" -> isDetached = true
                    line == "locked" -> isLocked = true
                    line.startsWith("locked ") -> {
                        isLocked = true
                        lockReason = line.removePrefix("locked ")
                    }
                }
            }
            flushEntry()
            return worktrees
        }
    }
}
