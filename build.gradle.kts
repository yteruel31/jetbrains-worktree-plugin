plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog") version "2.5.0"
}

group = "com.github.yoannteruel"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmDefault.set(org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY)
    }
}

changelog {
    version.set(providers.gradleProperty("pluginVersion"))
    path.set(file("CHANGELOG.md").canonicalPath)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")

        bundledPlugin("Git4Idea")

        pluginVerifier()
        instrumentationTools()
        jetbrainsRuntime()

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.JUnit5)
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    projectName = "jetbrains-worktree-plugin"

    pluginConfiguration {
        id = "com.github.yoannteruel.jetbrainsworktreeplugin"
        name = "Git Worktree Tool"
        version = project.version.toString()
        description = """
            <p>Manage Git worktrees directly from your IDE — create, remove, open, lock, move, and compare worktrees without leaving your editor.</p>
            <h3>Features</h3>
            <ul>
                <li>Create and remove worktrees from a dedicated VCS tab</li>
                <li>Open worktrees in new IDE windows</li>
                <li>Lock/unlock and move worktrees</li>
                <li>Sync files and settings across worktrees (<code>.idea</code>, <code>.env</code>, and custom paths)</li>
                <li>Checkout pull/merge requests as worktrees</li>
                <li>Compare worktree branches with built-in diff viewer</li>
                <li>Create worktrees from any commit via VCS Log context menu</li>
                <li>Per-dialog overrides for sync, post-creation command, and open-in-new-window</li>
            </ul>
            <h3>Requirements</h3>
            <ul>
                <li>Git 2.15+ (for worktree support)</li>
                <li>IntelliJ IDEA 2025.1+ or any JetBrains IDE</li>
            </ul>
        """.trimIndent()
        vendor {
            name = "Yoann Teruel"
        }

        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }

        changeNotes = provider {
            changelog.renderItem(
                changelog.get(providers.gradleProperty("pluginVersion").get()),
                org.jetbrains.changelog.Changelog.OutputType.HTML
            )
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
