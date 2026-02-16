package com.github.yoannteruel.jetbrainsworktreeplugin.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

class WorktreeSyncServiceTest {

    @TempDir
    lateinit var tempDir: Path

    // === .idea sync tests (existing) ===

    @Test
    fun `copies idea files to target directory`() {
        val source = tempDir.resolve("source/.idea")
        val target = tempDir.resolve("target/.idea")
        Files.createDirectories(source)

        Files.writeString(source.resolve("misc.xml"), "<project/>")
        Files.writeString(source.resolve("modules.xml"), "<modules/>")

        syncIdea(source, target, emptySet())

        assertTrue(Files.exists(target.resolve("misc.xml")))
        assertTrue(Files.exists(target.resolve("modules.xml")))
        assertEquals("<project/>", Files.readString(target.resolve("misc.xml")))
    }

    @Test
    fun `excludes specified files`() {
        val source = tempDir.resolve("source/.idea")
        val target = tempDir.resolve("target/.idea")
        Files.createDirectories(source)

        Files.writeString(source.resolve("misc.xml"), "<project/>")
        Files.writeString(source.resolve("workspace.xml"), "<workspace/>")

        syncIdea(source, target, setOf("workspace.xml"))

        assertTrue(Files.exists(target.resolve("misc.xml")))
        assertFalse(Files.exists(target.resolve("workspace.xml")))
    }

    @Test
    fun `excludes specified directories`() {
        val source = tempDir.resolve("source/.idea")
        val target = tempDir.resolve("target/.idea")
        val shelfDir = source.resolve("shelf")
        Files.createDirectories(shelfDir)

        Files.writeString(source.resolve("misc.xml"), "<project/>")
        Files.writeString(shelfDir.resolve("change.patch"), "diff content")

        syncIdea(source, target, setOf("shelf"))

        assertTrue(Files.exists(target.resolve("misc.xml")))
        assertFalse(Files.exists(target.resolve("shelf")))
    }

    @Test
    fun `preserves subdirectory structure`() {
        val source = tempDir.resolve("source/.idea")
        val target = tempDir.resolve("target/.idea")
        val subDir = source.resolve("inspectionProfiles")
        Files.createDirectories(subDir)

        Files.writeString(subDir.resolve("Project_Default.xml"), "<inspections/>")

        syncIdea(source, target, emptySet())

        assertTrue(Files.exists(target.resolve("inspectionProfiles/Project_Default.xml")))
        assertEquals("<inspections/>", Files.readString(target.resolve("inspectionProfiles/Project_Default.xml")))
    }

    @Test
    fun `overwrites existing files in target`() {
        val source = tempDir.resolve("source/.idea")
        val target = tempDir.resolve("target/.idea")
        Files.createDirectories(source)
        Files.createDirectories(target)

        Files.writeString(source.resolve("misc.xml"), "<new/>")
        Files.writeString(target.resolve("misc.xml"), "<old/>")

        syncIdea(source, target, emptySet())

        assertEquals("<new/>", Files.readString(target.resolve("misc.xml")))
    }

    @Test
    fun `handles empty source directory`() {
        val source = tempDir.resolve("source/.idea")
        val target = tempDir.resolve("target/.idea")
        Files.createDirectories(source)

        syncIdea(source, target, emptySet())

        assertTrue(Files.exists(target))
    }

    // === Single file sync tests ===

    @Test
    fun `copies single file to target worktree`() {
        val source = tempDir.resolve("source")
        val target = tempDir.resolve("target")
        Files.createDirectories(source)
        Files.createDirectories(target)
        Files.writeString(source.resolve(".env"), "SECRET=value")

        syncFile(source, target, ".env")

        assertTrue(Files.exists(target.resolve(".env")))
        assertEquals("SECRET=value", Files.readString(target.resolve(".env")))
    }

    @Test
    fun `creates parent directories for single file`() {
        val source = tempDir.resolve("source")
        val target = tempDir.resolve("target")
        Files.createDirectories(source.resolve("config"))
        Files.writeString(source.resolve("config/.npmrc"), "registry=https://example.com")

        syncFile(source, target, "config/.npmrc")

        assertTrue(Files.exists(target.resolve("config/.npmrc")))
        assertEquals("registry=https://example.com", Files.readString(target.resolve("config/.npmrc")))
    }

    @Test
    fun `overwrites existing single file in target`() {
        val source = tempDir.resolve("source")
        val target = tempDir.resolve("target")
        Files.createDirectories(source)
        Files.createDirectories(target)
        Files.writeString(source.resolve(".env"), "NEW=value")
        Files.writeString(target.resolve(".env"), "OLD=value")

        syncFile(source, target, ".env")

        assertEquals("NEW=value", Files.readString(target.resolve(".env")))
    }

    // === Custom directory sync tests ===

    @Test
    fun `copies custom directory recursively`() {
        val source = tempDir.resolve("source")
        val target = tempDir.resolve("target")
        val claudeDir = source.resolve(".claude")
        Files.createDirectories(claudeDir.resolve("sub"))
        Files.writeString(claudeDir.resolve("config.json"), "{}")
        Files.writeString(claudeDir.resolve("sub/data.json"), "[]")

        syncDirectory(source, target, ".claude", emptySet())

        assertTrue(Files.exists(target.resolve(".claude/config.json")))
        assertEquals("{}", Files.readString(target.resolve(".claude/config.json")))
        assertTrue(Files.exists(target.resolve(".claude/sub/data.json")))
        assertEquals("[]", Files.readString(target.resolve(".claude/sub/data.json")))
    }

    @Test
    fun `skips sync when source path does not exist`() {
        val source = tempDir.resolve("source")
        val target = tempDir.resolve("target")
        Files.createDirectories(source)
        Files.createDirectories(target)

        // Should not throw
        syncFile(source, target, ".nonexistent")

        assertFalse(Files.exists(target.resolve(".nonexistent")))
    }

    @Test
    fun `skips directory sync when source does not exist`() {
        val source = tempDir.resolve("source")
        val target = tempDir.resolve("target")
        Files.createDirectories(source)
        Files.createDirectories(target)

        // Should not throw
        syncDirectory(source, target, ".missing", emptySet())

        assertFalse(Files.exists(target.resolve(".missing")))
    }

    // === Helpers ===

    /**
     * Standalone sync implementation that mirrors IdeaSyncService logic
     * without requiring IntelliJ project context.
     */
    private fun syncIdea(sourceIdeaDir: Path, targetIdeaDir: Path, excluded: Set<String>) {
        Files.walkFileTree(sourceIdeaDir, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (dir != sourceIdeaDir && dir.fileName.toString() in excluded) {
                    return FileVisitResult.SKIP_SUBTREE
                }
                val targetDir = targetIdeaDir.resolve(sourceIdeaDir.relativize(dir))
                Files.createDirectories(targetDir)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.fileName.toString() in excluded) {
                    return FileVisitResult.CONTINUE
                }
                val targetFile = targetIdeaDir.resolve(sourceIdeaDir.relativize(file))
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun syncFile(basePath: Path, targetPath: Path, relativePath: String) {
        val sourceFile = basePath.resolve(relativePath)
        if (!Files.exists(sourceFile)) return
        val targetFile = targetPath.resolve(relativePath)
        Files.createDirectories(targetFile.parent)
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun syncDirectory(basePath: Path, targetPath: Path, relativePath: String, excluded: Set<String>) {
        val sourceDir = basePath.resolve(relativePath)
        val targetDir = targetPath.resolve(relativePath)
        if (!Files.exists(sourceDir)) return
        Files.walkFileTree(sourceDir, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (dir != sourceDir && dir.fileName.toString() in excluded) {
                    return FileVisitResult.SKIP_SUBTREE
                }
                Files.createDirectories(targetDir.resolve(sourceDir.relativize(dir)))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.fileName.toString() in excluded) return FileVisitResult.CONTINUE
                val target = targetDir.resolve(sourceDir.relativize(file))
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }
}
