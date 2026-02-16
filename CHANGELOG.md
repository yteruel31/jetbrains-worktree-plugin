# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [0.4.0] - 2026-02-16

### Added

- Sync custom files and directories across worktrees (`.env`, `.claude/`, `.npmrc`, and any user-defined paths)
- Unified Sync Configuration panel in settings with per-path enable/disable
- `.idea` sync exclusions dialog accessible from the sync configuration panel

### Changed

- Rename "Sync .idea Settings" action to "Sync to Worktrees" — now syncs all enabled entries
- "Auto-sync .idea on worktree creation" setting renamed to "Auto-sync on worktree creation" — applies to all enabled sync entries
- Refactor `IdeaSyncService` into `WorktreeSyncService` with support for both files and directories
- Worktrees now always open in a new window to prevent IDE crash during project switch

### Removed

- Remove "Open worktrees in new window" setting (was under Tools > Git Worktree Tool)

### Fixed

- Add disposed-project guards in FrameTitleBuilder, ProjectViewDecorator, cache refresh, and panel coroutines

## [0.3.1] - 2026-02-14

### Added

- Built-in error reporting: unhandled exceptions show a "Report to Plugin Vendor" button that opens a pre-filled GitHub issue using the bug report template

## [0.3.0] - 2026-02-14

### Changed

- Replace worktree table with a tree view showing main worktree as root with linked worktrees nested underneath
- Move worktree panel into the VCS tool window as a "Worktrees" tab next to the Log tab
- Toolbar actions now displayed on the left side, matching the Log tab style

### Fixed

- Fix invalid icon reference (`AllIcons.Actions.MoveTo`) on Move Worktree action by using a custom SVG icon
- Fix synchronous git execution on EDT in Compare Worktrees, Sync .idea, and Move Worktree actions
- Fix Move Worktree dialog validation always rejecting input due to `bindText` not syncing before validation

## [0.2.0] - 2026-02-14

### Added

- Branch name autocomplete with existing local branches in worktree creation dialogs
- Remote branches available in base branch selection
- Automatic space-to-dash replacement when typing branch names

### Fixed

- Migrate from deprecated `DataProvider.getData()` to modern `UiDataProvider.uiDataSnapshot()` API
- Eliminate Kotlin 2.2 compiler-generated bridge methods that triggered Plugin Verifier warnings for `ToolWindowFactory` deprecated/experimental defaults

## [0.1.1] - 2026-02-12

### Fixed

- Fix deadlock caused by synchronous git process execution under ReadAction in project view decorator and frame title builder
- Fix invalid icon reference (`AllIcons.Actions.Move`) causing `PluginException` on Move Worktree action

## [0.1.0] - 2025-02-09

### Added

- Git worktree list, create, remove, open, lock/unlock, and move
- `.idea` settings sync from main worktree to linked worktrees
- Worktree comparison view
- VCS Log context menu: create worktree from a commit
- Pull/merge request checkout as worktree (GitHub, GitLab, Bitbucket)
- Post-creation command hooks
- Worktree name in IDE title bar and project tree
- Configurable settings under **Tools > Git Worktree Tool**
