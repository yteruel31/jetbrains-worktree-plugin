# CLAUDE.md — jetbrains-worktree-plugin

## Project

JetBrains IDE plugin for Git worktree management. Kotlin/JVM, built with Gradle and the IntelliJ Platform Plugin SDK.

## Tech Stack

- **Kotlin** 2.2.x (K2 mode) / **Java** 21
- **Gradle** 8.13 with Kotlin DSL
- **IntelliJ Platform Gradle Plugin** 2.11.0 (`org.jetbrains.intellij.platform`)
- **Target IDE**: IntelliJ IDEA Community 2025.1+ (all JetBrains IDEs via `com.intellij.modules.platform`)
- **Plugin dependencies**: `Git4Idea` (bundled)

## Build & Run

Requires JDK 21. The Gradle toolchain (`jvmToolchain(21)` in `build.gradle.kts`) auto-detects a compatible JDK. If auto-detection fails, set `JAVA_HOME` explicitly.

```bash
./gradlew build       # Build
./gradlew test        # Run tests
./gradlew runIde      # Launch sandbox IDE with plugin installed
```

## Project Structure

```
com.github.yoannteruel.jetbrainsworktreeplugin
  model/          WorktreeInfo data class
  services/       GitWorktreeService (git worktree CLI wrapper), IdeaSyncService (.idea sync)
  ui/             Tool window factory, panel, table model, dialogs
  actions/        AnAction subclasses (Create, Remove, Open, Lock, Move, Sync, Compare, Refresh)
  settings/       Persistent settings state, service, configurable (under Tools)
```

Key files:
- `src/main/resources/META-INF/plugin.xml` — all registrations (services, tool window, actions, settings)
- `build.gradle.kts` — build configuration, dependencies
- `settings.gradle.kts` — IntelliJ Platform repository setup

## Architecture Notes

- Git worktree commands run via `GeneralCommandLine` + `CapturingProcessHandler` (since `GitCommand` has no `WORKTREE` constant)
- `WorktreeListChangedTopic` on `MessageBus` for panel auto-refresh after mutations
- Actions get selected worktree from `DataContext` via `WorktreeToolWindowPanel.SELECTED_WORKTREE` DataKey
- Background work uses `ProgressManager` / `Task.Backgroundable`
- Settings persisted via `SimplePersistentStateComponent` to `gitWorktreeManager.xml`

