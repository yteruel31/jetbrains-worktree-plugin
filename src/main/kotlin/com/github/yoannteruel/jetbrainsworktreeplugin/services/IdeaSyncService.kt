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
class IdeaSyncService(private val project: Project) {

    companion object {
        private val LOG = logger<IdeaSyncService>()

        private val DEFAULT_EXCLUDED = setOf(
            "workspace.xml",
            "shelf",
            "dataSources",
            "httpRequests",
            "dictionaries",
        )

        fun getInstance(project: Project): IdeaSyncService = project.service()
    }

    fun syncToWorktree(targetWorktreePath: String) {
        val basePath = project.basePath ?: return
        val sourceIdeaDir = Path.of(basePath, ".idea")
        val targetIdeaDir = Path.of(targetWorktreePath, ".idea")

        if (!sourceIdeaDir.exists()) {
            LOG.warn("Source .idea directory does not exist: $sourceIdeaDir")
            return
        }

        val excluded = getExcludedNames()

        try {
            Files.walkFileTree(sourceIdeaDir, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir != sourceIdeaDir && dir.name in excluded) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    val targetDir = targetIdeaDir.resolve(sourceIdeaDir.relativize(dir))
                    Files.createDirectories(targetDir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (file.name in excluded) {
                        return FileVisitResult.CONTINUE
                    }
                    val targetFile = targetIdeaDir.resolve(sourceIdeaDir.relativize(file))
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    return FileVisitResult.CONTINUE
                }
            })

            val targetVf = VfsUtil.findFile(targetIdeaDir, true)
            if (targetVf != null) {
                VfsUtil.markDirtyAndRefresh(true, true, true, targetVf)
            }
        } catch (e: IOException) {
            LOG.error("Failed to sync .idea to $targetWorktreePath", e)
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

    private fun getExcludedNames(): Set<String> {
        return try {
            val settings = WorktreeSettingsService.getInstance(project)
            val exclusions = settings.state.ideaSyncExclusions
            val custom = exclusions
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                .orEmpty()
            DEFAULT_EXCLUDED + custom
        } catch (_: Exception) {
            DEFAULT_EXCLUDED
        }
    }
}
