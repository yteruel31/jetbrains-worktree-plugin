package com.github.yoannteruel.jetbrainsworktreeplugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GitWorktreeErrorReportSubmitterTest {

    @Test
    fun `sanitizePii replaces home directory`() {
        val home = System.getProperty("user.home")
        val input = "at com.example.Foo(${home}/project/Foo.kt:42)"
        val result = GitWorktreeErrorReportSubmitter.sanitizePii(input)
        assertEquals("at com.example.Foo(<HOME>/project/Foo.kt:42)", result)
        assertFalse(result.contains(home))
    }

    @Test
    fun `sanitizePii replaces multiple occurrences`() {
        val home = System.getProperty("user.home")
        val input = "path1=${home}/a path2=${home}/b"
        val result = GitWorktreeErrorReportSubmitter.sanitizePii(input)
        assertEquals("path1=<HOME>/a path2=<HOME>/b", result)
    }

    @Test
    fun `sanitizePii handles text without home directory`() {
        val input = "at com.example.Foo(Foo.kt:42)"
        val result = GitWorktreeErrorReportSubmitter.sanitizePii(input)
        assertEquals(input, result)
    }

    @Test
    fun `truncate returns original when under limit`() {
        val input = "short text"
        assertEquals(input, GitWorktreeErrorReportSubmitter.truncate(input, 100))
    }

    @Test
    fun `truncate returns original when exactly at limit`() {
        val input = "a".repeat(50)
        assertEquals(input, GitWorktreeErrorReportSubmitter.truncate(input, 50))
    }

    @Test
    fun `truncate cuts and appends marker when over limit`() {
        val input = "a".repeat(200)
        val result = GitWorktreeErrorReportSubmitter.truncate(input, 50)
        assertTrue(result.startsWith("a".repeat(50)))
        assertTrue(result.endsWith("... (truncated)"))
        assertEquals(50 + "\n... (truncated)".length, result.length)
    }
}
