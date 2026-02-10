plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform")
}

group = "com.github.yoannteruel"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
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
        description = "Git worktree management for JetBrains IDEs"
        vendor {
            name = "Yoann Teruel"
        }

        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }

        changeNotes = provider {
            file("CHANGELOG.md").readText()
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
