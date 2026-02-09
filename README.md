# Git Worktree Manager

JetBrains IDE plugin for Git worktree management. Create, remove, open, lock/unlock, move worktrees and sync `.idea` settings — all from the IDE.

[![Build](https://github.com/yteruel31/jetbrains-worktree-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/yteruel31/jetbrains-worktree-plugin/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2025.1%2B-blue)

## Features

- **Worktree management** — list, create, remove, open, lock/unlock, and move Git worktrees
- **Open in new window** — open any worktree in a separate IDE window
- **.idea sync** — sync IDE settings from the main worktree to all linked worktrees
- **Compare worktrees** — side-by-side comparison view
- **VCS Log integration** — right-click a commit in the Git log to create a worktree from it
- **PR checkout** — create a worktree from a pull/merge request (GitHub, GitLab, Bitbucket)
- **Post-creation hooks** — run custom commands after creating a worktree
- **IDE integration** — worktree name shown in the title bar and project tree

## Installation

### From JetBrains Marketplace

> Coming soon

### Manual

1. Download the latest release from [Releases](https://github.com/yteruel31/jetbrains-worktree-plugin/releases)
2. In your IDE, go to **Settings > Plugins > Install Plugin from Disk...**
3. Select the downloaded `.zip` file

## Usage

After installation, a **Git Worktrees** tool window appears in the bottom panel. From there you can manage all your worktrees.

Configure the plugin under **Settings > Tools > Git Worktree Manager**.

## Building from Source

**Prerequisites:** JDK 21

```bash
./gradlew build       # Build the plugin
./gradlew test        # Run tests
./gradlew runIde      # Launch a sandbox IDE with the plugin installed
```

The Gradle toolchain auto-detects a compatible JDK. If that fails, set `JAVA_HOME` to your JDK 21 installation.

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

[MIT](LICENSE)
