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

Requires a JDK 21 installation. Set `JAVA_HOME` before running Gradle commands.

```bash
# Build
JAVA_HOME="/Applications/PyCharm.app/Contents/jbr/Contents/Home" ./gradlew build

# Run tests
JAVA_HOME="/Applications/PyCharm.app/Contents/jbr/Contents/Home" ./gradlew test

# Launch sandbox IDE with plugin installed
JAVA_HOME="/Applications/PyCharm.app/Contents/jbr/Contents/Home" ./gradlew runIde
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

## Agent Team Guidelines

### When to use agent teams

Use agent teams for tasks where parallel, independent work adds value:

- Researching multiple APIs or libraries simultaneously (e.g. IntelliJ Platform SDK + Git CLI + VFS)
- Implementing independent modules (UI panel, Git service layer, settings, tests)
- Code review with separate reviewers for security, performance, and test coverage
- Debugging with competing hypotheses tested in parallel
- Cross-layer changes (plugin actions, services, persistence, UI)

Do NOT use agent teams for:

- Sequential tasks where each step depends on the previous
- Edits to the same file from multiple teammates
- Simple single-file fixes or small changes

### Team structure patterns

**Feature implementation (3-4 teammates):**
- One teammate per independent module/layer
- Example: UI components, service layer, plugin.xml registration, tests

**Research phase (2-3 teammates):**
- Each teammate investigates a different aspect
- Example: IntelliJ Platform API research, Git worktree CLI behavior, existing plugin analysis

**Code review (3 teammates):**
- Security reviewer, performance reviewer, test coverage reviewer
- Each applies a different lens to the same code

### File ownership

Assign clear file ownership to avoid conflicts. Never have two teammates edit the same file. Break work along natural boundaries:

- `src/main/kotlin/.../services/` — one teammate
- `src/main/kotlin/.../ui/` — another teammate
- `src/main/kotlin/.../actions/` — another teammate
- `src/test/` — dedicated test teammate, starts after implementation teammates finish

### Task sizing

- Aim for 5-6 tasks per teammate
- Each task should produce a clear deliverable (a class, a test file, a config)
- Too small = coordination overhead exceeds benefit
- Too large = teammates work too long without check-ins

### Coordination rules

- Use **delegate mode** (Shift+Tab) to keep the lead focused on orchestration
- Require **plan approval** for risky or architectural changes before teammates implement
- Tell the lead to **wait for teammates** before synthesizing — it sometimes starts working itself
- Check in on teammates periodically; redirect approaches that aren't working

### Research preferences

- Use Google Research plugin for all web searches (WebSearch tool is disabled)
- Teammates should use `mcp__plugin_google-research_google-research__research` for looking up IntelliJ Platform SDK docs, Kotlin APIs, or any external information

### Context for teammates

When spawning teammates, always include in the spawn prompt:

- Which files/directories they own
- What specific deliverable they should produce
- Relevant technical constraints (IntelliJ Platform version, Kotlin version, API patterns)
- Dependencies on other teammates' work

### Cleanup

- Always shut down all teammates before asking the lead to clean up
- Use the lead (not a teammate) to run cleanup
- Check for orphaned tmux sessions if using split-pane mode: `tmux ls`