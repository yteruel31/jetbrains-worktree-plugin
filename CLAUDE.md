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
  services/       GitWorktreeService (CLI wrapper + caching), IdeaSyncService, WorktreeCacheStartupActivity
  ui/             VCS tab provider, panel, tree cell renderer, dialogs, frame title builder, project view decorator
  actions/        AnAction subclasses (Create, CreateFromCommit, Remove, Open, Lock, Move, Sync, Compare, CheckoutPR, Refresh)
  settings/       Persistent settings state, service, configurable (under Tools)
```

Key files:
- `src/main/resources/META-INF/plugin.xml` — all registrations (services, actions, settings, extension points)
- `build.gradle.kts` — build configuration, dependencies
- `settings.gradle.kts` — IntelliJ Platform repository setup

## Architecture Notes

- Worktree panel lives as a **tab in the VCS tool window** via `changesViewContent` extension point (`WorktreeChangesViewContentProvider`), not a standalone tool window
- Worktree panel uses a `Tree` with `DefaultTreeModel`: main worktree as root node (always expanded), linked worktrees as children. `ColoredTreeCellRenderer` handles node presentation. `TreeSpeedSearch` provides type-to-filter.
- Git worktree commands run via `GeneralCommandLine` + `CapturingProcessHandler` (since `GitCommand` has no `WORKTREE` constant)
- `WorktreeListChangedTopic` on `MessageBus` for panel auto-refresh after mutations
- Actions get selected worktree from `DataContext` via `WorktreeToolWindowPanel.SELECTED_WORKTREE` DataKey
- Background work uses `ProgressManager` / `Task.Backgroundable`
- Settings persisted via `SimplePersistentStateComponent` to `gitWorktreeManager.xml`
- Worktree list cached in `GitWorktreeService` with async refresh via `WorktreeCacheStartupActivity`
- `WorktreeFrameTitleBuilder` decorates the IDE title bar with worktree branch name
- `WorktreeProjectViewDecorator` adds branch labels to the project tree

## Testing

- **Framework**: JUnit 5 (5.11.4)
- **Location**: `src/test/kotlin/`
- **Coverage**: worktree parser, model, .idea sync logic

## CI

- `.github/workflows/build.yml` — builds and runs `verifyPlugin` on push/PR to main
- `.github/workflows/release.yml` — builds, signs, and publishes to JetBrains Marketplace

## Development Guidelines

- **Don't over-engineer.** Keep implementations minimal and direct. Don't add abstractions, wrappers, utility classes, or extra layers unless clearly justified by an immediate need. Simple, straightforward code is preferred over clever indirection.
- **Use native IntelliJ Platform APIs first.** Always prefer built-in SDK APIs over custom implementations or third-party libraries. If you're about to write a "manager" or "utility" for UI, threading, persistence, or notifications — there is likely a native API you should use instead:
  - `Kotlin UI DSL v2` (`panel`, `row`, `cell`) — not raw Swing layouts (`GridBagLayout`, `GroupLayout`)
  - `PersistentStateComponent` — not custom config/JSON/XML files
  - `ProgressManager` / `Task.Backgroundable` — not manual threading
  - `NotificationGroupManager` + `Notification` — not custom popup UIs
  - `MessageBus` — not custom listener/callback patterns
  - `GeneralCommandLine` + `CapturingProcessHandler` — for process execution
  - `IconLoader` / `AllIcons` — for icons (supports HiDPI and themes)
- **Never block the EDT.** All I/O, git commands, and heavy computation must run in `Task.Backgroundable` or coroutines.
- **No internal APIs.** Don't use classes from `*.impl` packages or annotated `@ApiStatus.Internal`.
- **Keep the plugin description up to date.** When adding or removing user-facing features, update the HTML `description` in `build.gradle.kts` (`pluginConfiguration` block) to reflect the change. This description is shown on the JetBrains Marketplace and in the IDE Plugins dialog.
