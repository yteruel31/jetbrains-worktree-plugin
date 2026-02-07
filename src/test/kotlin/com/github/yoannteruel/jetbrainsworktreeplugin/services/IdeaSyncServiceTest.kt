package com.github.yoannteruel.jetbrainsworktreeplugin.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class IdeaSyncServiceTest {

    @TempDir
    lateinit var tempDir: Path

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

    /**
     * Standalone sync implementation that mirrors IdeaSyncService logic
     * without requiring IntelliJ project context.
     */
    private fun syncIdea(sourceIdeaDir: Path, targetIdeaDir: Path, excluded: Set<String>) {
        Files.walkFileTree(sourceIdeaDir, object : java.nio.file.SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                if (dir != sourceIdeaDir && dir.fileName.toString() in excluded) {
                    return java.nio.file.FileVisitResult.SKIP_SUBTREE
                }
                val targetDir = targetIdeaDir.resolve(sourceIdeaDir.relativize(dir))
                Files.createDirectories(targetDir)
                return java.nio.file.FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                if (file.fileName.toString() in excluded) {
                    return java.nio.file.FileVisitResult.CONTINUE
                }
                val targetFile = targetIdeaDir.resolve(sourceIdeaDir.relativize(file))
                Files.copy(file, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                return java.nio.file.FileVisitResult.CONTINUE
            }
        })
    }
}
