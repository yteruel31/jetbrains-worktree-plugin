package com.github.yoannteruel.jetbrainsworktreeplugin.services

import com.github.yoannteruel.jetbrainsworktreeplugin.model.WorktreeInfo
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.config.GitExecutableManager
import git4idea.repo.GitRepositoryManager

interface WorktreeListChangeListener {
    fun worktreeListChanged()
}

@Service(Service.Level.PROJECT)
class GitWorktreeService(private val project: Project) {

    companion object {
        private val LOG = logger<GitWorktreeService>()

        val TOPIC: Topic<WorktreeListChangeListener> = Topic.create(
            "GitWorktreeListChanged",
            WorktreeListChangeListener::class.java
        )

        fun getInstance(project: Project): GitWorktreeService = project.service()
    }

    fun listWorktrees(): List<WorktreeInfo> {
        val output = runWorktreeCommand("list", "--porcelain") ?: return emptyList()
        return parseWorktreeListOutput(output.lines())
    }

    fun addWorktree(path: String, branch: String?, createNewBranch: Boolean, baseBranch: String?) {
        val args = mutableListOf("add")
        if (createNewBranch && branch != null) {
            args.addAll(listOf("-b", branch))
        }
        args.add(path)
        if (!createNewBranch && branch != null) {
            args.add(branch)
        } else if (baseBranch != null) {
            args.add(baseBranch)
        }
        runWorktreeCommand(*args.toTypedArray())
        fireWorktreeListChanged()
    }

    fun removeWorktree(path: String, force: Boolean = false) {
        val args = mutableListOf("remove")
        if (force) args.add("--force")
        args.add(path)
        runWorktreeCommand(*args.toTypedArray())
        fireWorktreeListChanged()
    }

    fun lockWorktree(path: String, reason: String?) {
        val args = mutableListOf("lock")
        if (reason != null) {
            args.addAll(listOf("--reason", reason))
        }
        args.add(path)
        runWorktreeCommand(*args.toTypedArray())
        fireWorktreeListChanged()
    }

    fun unlockWorktree(path: String) {
        runWorktreeCommand("unlock", path)
        fireWorktreeListChanged()
    }

    fun moveWorktree(path: String, newPath: String) {
        runWorktreeCommand("move", path, newPath)
        fireWorktreeListChanged()
    }

    fun getAvailableBranches(): List<String> {
        val root = findGitRoot() ?: return emptyList()
        val handler = GitLineHandler(project, root, GitCommand.BRANCH)
        handler.addParameters("--list", "--format=%(refname:short)")
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) return emptyList()
        return result.output.filter { it.isNotBlank() }
    }

    internal fun parseWorktreeListOutput(lines: List<String>): List<WorktreeInfo> {
        val worktrees = mutableListOf<WorktreeInfo>()
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
                WorktreeInfo(
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

    private fun runWorktreeCommand(vararg args: String): String? {
        val root = findGitRoot() ?: return null
        val gitExecutable = GitExecutableManager.getInstance().getPathToGit(project)
        val cmd = GeneralCommandLine(gitExecutable, "worktree", *args)
        cmd.withWorkDirectory(root.path)
        return try {
            val handler = CapturingProcessHandler(cmd)
            val result = handler.runProcess(60_000)
            if (result.exitCode == 0) {
                result.stdout
            } else {
                LOG.warn("git worktree ${args.firstOrNull()} failed: ${result.stderr}")
                null
            }
        } catch (e: Exception) {
            LOG.error("Failed to run git worktree command", e)
            null
        }
    }

    private fun findGitRoot(): com.intellij.openapi.vfs.VirtualFile? {
        val repos = GitRepositoryManager.getInstance(project).repositories
        return repos.firstOrNull()?.root
    }

    private fun fireWorktreeListChanged() {
        project.messageBus.syncPublisher(TOPIC).worktreeListChanged()
    }
}
