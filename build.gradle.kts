import com.typewritermc.moduleplugin.ReleaseChannel

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.0.0"
}

val astralCoreVersion: String by project

group = "com.astralrealms"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.typewritermc.com/releases")

    // AstralCore lives in the private AstralRealms repository. Credentials are optional: with a
    // local `mvn install` of AstralCore, mavenLocal() above already resolves it.
    listOf(
        "https://maven.astralrealms.fr/repository/maven-snapshots/",
        "https://maven.astralrealms.fr/repository/maven-releases/",
    ).forEach { repoUrl ->
        maven(repoUrl) {
            content { includeGroup("com.astralrealms") }
            val user = providers.gradleProperty("astralRepoUsername").orElse(providers.environmentVariable("ASTRAL_REPO_USERNAME"))
            val pass = providers.gradleProperty("astralRepoPassword").orElse(providers.environmentVariable("ASTRAL_REPO_PASSWORD"))
            if (user.isPresent && pass.isPresent) {
                credentials {
                    username = user.get()
                    password = pass.get()
                }
            }
        }
    }
}

// The Typewriter engine pulls in EntityLib and Geyser from repositories that are currently
// unreachable, and neither is needed by this extension. Excluding them keeps resolution offline-safe.
configurations.configureEach {
    exclude(group = "me.tofaa.entitylib")
    exclude(group = "org.geysermc.geyser")
}

dependencies {
    // The shaded AstralCore paper jar already contains core-commons, so nothing transitive is needed.
    compileOnly("com.astralrealms:core-paper:$astralCoreVersion") { isTransitive = false }
}

typewriter {
    namespace = "astralrealms"

    extension {
        name = "AstralCore"
        shortDescription = "Bridge Typewriter with the AstralCore Paper module."
        description = """
            |Bridges Typewriter and the AstralCore Paper module in both directions.
            |
            |From Typewriter you can run AstralCore action lists, gate audiences and criteria on
            |AstralCore requirements, and read AstralCore placeholders, inline functions and
            |expressions as Typewriter variables and facts.
            |
            |From AstralCore configuration (menus, dialogs, items) you can trigger Typewriter
            |entries, read Typewriter facts as placeholders and functions, and gate anything on a
            |Typewriter fact.
        """.trimMargin()
        engineVersion = "0.9.0"
        channel = ReleaseChannel.RELEASE

        paper {
            dependency("AstralCore")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

/**
 * Copies the built extension into a server's `plugins/Typewriter/extensions` folder.
 *
 * Point it at a server with `-PtypewriterServerDir=/path/to/server`, or set `typewriterServerDir`
 * in `gradle.properties` (or `~/.gradle/gradle.properties`) so a plain `gradle buildAndMove` works.
 */
tasks.register<Copy>("buildAndMove") {
    group = "build"
    description = "Builds the extension and copies it into a Typewriter server's extensions folder."

    val serverDir = providers.gradleProperty("typewriterServerDir")
    onlyIf {
        if (!serverDir.isPresent) {
            logger.error("No 'typewriterServerDir' set. Pass -PtypewriterServerDir=/path/to/server.")
        }
        serverDir.isPresent
    }

    from(tasks.jar)
    into(serverDir.map { "$it/plugins/Typewriter/extensions" })
    rename { "AstralCoreBridge.jar" }
}
