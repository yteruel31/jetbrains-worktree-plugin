# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [0.1.1] - 2026-02-12

### Fixed

- Fix deadlock caused by synchronous git process execution under ReadAction in project view decorator and frame title builder

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
