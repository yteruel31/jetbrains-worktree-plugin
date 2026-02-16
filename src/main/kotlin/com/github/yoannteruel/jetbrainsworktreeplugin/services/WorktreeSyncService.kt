package com.github.yoannteruel.jetbrainsworktreeplugin.services

import com.github.yoannteruel.jetbrainsworktreeplugin.settings.WorktreeSettingsService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.name

@Service(Service.Level.PROJECT)
class WorktreeSyncService(private val project: Project) {

    companion object {
        private val LOG = logger<WorktreeSyncService>()

        private val IDEA_DEFAULT_EXCLUDED = setOf(
            "workspace.xml",
            "shelf",
            "dataSources",
            "httpRequests",
            "dictionaries",
        )

        fun getInstance(project: Project): WorktreeSyncService = project.service()
    }

    fun syncToWorktree(targetWorktreePath: String) {
        val basePath = project.basePath ?: return
        val settings = WorktreeSettingsService.getInstance(project)
        val state = settings.state

        if (state.ideaSyncEnabled) {
            syncDirectoryToWorktree(basePath, ".idea", targetWorktreePath, getIdeaExcludedNames())
        }

        for (entry in state.customSyncEntries) {
            if (!entry.enabled || entry.path.isNullOrBlank()) continue
            syncPathToWorktree(basePath, entry.path!!.trim(), targetWorktreePath)
        }
    }

    fun syncToAllWorktrees() {
        val worktreeService = GitWorktreeService.getInstance(project)
        val worktrees = worktreeService.listWorktrees()
        for (worktree in worktrees) {
            if (!worktree.isMainWorktree) {
                syncToWorktree(worktree.path)
            }
        }
    }

    private fun syncPathToWorktree(basePath: String, relativePath: String, targetWorktreePath: String) {
        val sourcePath = Path.of(basePath, relativePath)
        if (!sourcePath.exists()) {
            LOG.info("Sync source does not exist, skipping: $sourcePath")
            return
        }
        if (Files.isDirectory(sourcePath)) {
            syncDirectoryToWorktree(basePath, relativePath, targetWorktreePath, excluded = emptySet())
        } else {
            syncFileToWorktree(basePath, relativePath, targetWorktreePath)
        }
    }

    private fun syncFileToWorktree(basePath: String, relativePath: String, targetWorktreePath: String) {
        val sourceFile = Path.of(basePath, relativePath)
        val targetFile = Path.of(targetWorktreePath, relativePath)
        try {
            Files.createDirectories(targetFile.parent)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
            val targetVf = VfsUtil.findFile(targetFile, true)
            if (targetVf != null) {
                VfsUtil.markDirtyAndRefresh(true, false, false, targetVf)
            }
        } catch (e: IOException) {
            LOG.error("Failed to sync file $relativePath to $targetWorktreePath", e)
        }
    }

    private fun syncDirectoryToWorktree(
        basePath: String,
        relativePath: String,
        targetWorktreePath: String,
        excluded: Set<String>
    ) {
        val sourceDir = Path.of(basePath, relativePath)
        val targetDir = Path.of(targetWorktreePath, relativePath)

        if (!sourceDir.exists()) {
            LOG.warn("Source directory does not exist: $sourceDir")
            return
        }

        try {
            Files.walkFileTree(sourceDir, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir != sourceDir && dir.name in excluded) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    val targetSubDir = targetDir.resolve(sourceDir.relativize(dir))
                    Files.createDirectories(targetSubDir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (file.name in excluded) {
                        return FileVisitResult.CONTINUE
                    }
                    val targetFile = targetDir.resolve(sourceDir.relativize(file))
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    return FileVisitResult.CONTINUE
                }
            })

            val targetVf = VfsUtil.findFile(targetDir, true)
            if (targetVf != null) {
                VfsUtil.markDirtyAndRefresh(true, true, true, targetVf)
            }
        } catch (e: IOException) {
            LOG.error("Failed to sync directory $relativePath to $targetWorktreePath", e)
        }
    }

    private fun getIdeaExcludedNames(): Set<String> {
        return try {
            val settings = WorktreeSettingsService.getInstance(project)
            val exclusions = settings.state.ideaSyncExclusions
            val custom = exclusions
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                .orEmpty()
            IDEA_DEFAULT_EXCLUDED + custom
        } catch (_: Exception) {
            IDEA_DEFAULT_EXCLUDED
        }
    }
}
