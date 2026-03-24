import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.therouter.cn:8443/repository/maven-public/")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.therouter.cn:8443/repository/maven-public/")
    }
}

rootProject.name = "TheRouter"

fun getLocalProperties(): Properties {
    val properties = Properties()
    try {
        val localPropertiesFile = File(rootDir, "local.properties")
        FileInputStream(localPropertiesFile).use { inputStream ->
            properties.load(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return properties
}

getLocalProperties().entries.forEach { entry ->
    if (entry.value.toString().toBoolean()) {
        val moduleName = entry.key.toString()
        if (moduleName.isNotEmpty()) {
            val file = file(moduleName)
            if (file.exists()) {
                include(":$moduleName")
            }
        }
    }
}
