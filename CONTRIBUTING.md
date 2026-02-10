# Contributing

Thanks for your interest in contributing to Git Worktree Tool!

## Getting Started

1. Fork and clone the repository
2. Ensure you have JDK 21 installed
3. Build the project: `./gradlew build`
4. Launch a sandbox IDE: `./gradlew runIde`

## Development Workflow

1. Create a feature branch from `main`
2. Make your changes
3. Run tests: `./gradlew test`
4. Test manually in the sandbox IDE: `./gradlew runIde`
5. Open a pull request against `main`

## Code Style

- Follow Kotlin coding conventions (IntelliJ default formatter)
- Follow existing patterns in the codebase
- Services use `GeneralCommandLine` + `CapturingProcessHandler` for Git CLI operations
- Actions extend `AnAction` and get the selected worktree via `DataContext`

## What to Contribute

- Bug reports and fixes
- New worktree-related features
- Test coverage improvements
- Documentation improvements

## Reporting Bugs

Use the [Bug Report](https://github.com/yteruel31/jetbrains-worktree-plugin/issues/new?template=bug_report.yml) issue template. Include your IDE version, plugin version, OS, and steps to reproduce.
