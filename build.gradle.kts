import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()
fun environment(key: String) = providers.environmentVariable(key)

// Import variables from gradle.properties file
val pluginGroup: String by project
val pluginName: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

val javaVersion: String by project

plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.gradleIntelliJPlugin)
  alias(libs.plugins.changelog)
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktlint)
}

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
  mavenCentral()
  mavenLocal()
  gradlePluginPortal()

  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaUltimate(platformVersion) {
      useInstaller = false
    }
    pluginVerifier()
    zipSigner()
  }
}

intellijPlatform {
  pluginConfiguration {
    id = pluginGroup
    name = pluginName
    version = pluginVersion

    ideaVersion {
      sinceBuild = pluginSinceBuild
      untilBuild = pluginUntilBuild
    }

    changeNotes = provider {
      changelog.run {
        renderItem(
          getOrNull(properties("pluginVersion")) ?: getLatest(),
          Changelog.OutputType.HTML,
        )
      }
    }
  }

  pluginVerification {
    ides {
      recommended()
      select {
        sinceBuild = pluginSinceBuild
        untilBuild = pluginUntilBuild
      }
    }
  }

  signing {
    certificateChain = environment("CERTIFICATE_CHAIN")
    privateKey = environment("PRIVATE_KEY")
    password = environment("PRIVATE_KEY_PASSWORD")
  }

  publishing {
    token = environment("PUBLISH_TOKEN")
    channels = listOf(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first())
  }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  path.set("${project.projectDir}/docs/CHANGELOG.md")
  version.set(pluginVersion)
  itemPrefix.set("-")
  keepUnreleasedSection.set(true)
  unreleasedTerm.set("[Unreleased]")
  groups.set(listOf("Features", "Fixes", "Other", "Bump"))
}


kotlin {
  jvmToolchain(25)
}

tasks {
  javaVersion.let {
    // Set the compatibility versions to 21
    withType<JavaCompile> {
      sourceCompatibility = "25"
      targetCompatibility = "25"
    }
  }

  wrapper {
    gradleVersion = properties("gradleVersion")
  }

  buildSearchableOptions {
    enabled = false
  }

  register("markdownToHtml") {
    val input = File("./docs/CHANGELOG.md")
    File("./docs/CHANGELOG.html").run {
      writeText(markdownToHTML(input.readText()))
    }
  }
}
