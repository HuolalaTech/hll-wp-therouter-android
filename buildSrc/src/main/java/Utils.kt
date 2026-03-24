import groovy.lang.Closure
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

fun Project.source(provider: Provider<MinimalExternalModuleDependency>) {
    val mod = provider.get()
    source(mod.group ?: "", mod.name, mod.version ?: "", null)
}

fun Project.source(compileStr: String, configureClosure: Closure<*>) {
    val parts = compileStr.split(":")
    val group = parts[0]
    val artifactid = parts[1]
    val version = parts[2]

    source(group, artifactid, version, configureClosure)
}

fun Project.source(group: String, artifactid: String, version: String) {
    source(group, artifactid, version, null)
}

fun Project.source(group: String, artifactid: String, version: String, configureClosure: Closure<*>?) {
    source("implementation", group, artifactid, version, configureClosure)
}

/////////////////////////////////////////////////////////////////////////////////////////

fun Project.sourceKsp(provider: Provider<MinimalExternalModuleDependency>) {
    val mod = provider.get()
    sourceKsp(mod.group ?: "", mod.name, mod.version ?: "", null)
}

fun Project.sourceKsp(compileStr: String, configureClosure: Closure<*>) {
    val parts = compileStr.split(":")
    val group = parts[0]
    val artifactid = parts[1]
    val version = parts[2]

    sourceKsp(group, artifactid, version, configureClosure)
}

fun Project.sourceKsp(group: String, artifactid: String, version: String) {
    sourceKsp(group, artifactid, version, null)
}

fun Project.sourceKsp(group: String, artifactid: String, version: String, configureClosure: Closure<*>?) {
    source("ksp", group, artifactid, version, configureClosure)
}

/////////////////////////////////////////////////////////////////////////////////////////

fun Project.source(type: String, group: String, artifactid: String, version: String, configureClosure: Closure<*>?) {
    val includeModule = rootProject.allprojects
        .filter { it != rootProject }
        .map { it.name }
        .toSet()

    var depVersion = version

    if (artifactid == "router" && this.name == "compose") {
        depVersion = this.version.toString()
    }

    if (includeModule.contains(artifactid)) {
        println("${project.name} $type project(\":$artifactid\")")
        configureClosure?.let {
            dependencies.add(type, project(":$artifactid"), configureClosure)
        } ?: let {
            dependencies.add(type, project(":$artifactid"))
        }
    } else {
        println("${project.name} $type $group:$artifactid:$depVersion")
        configureClosure?.let {
            dependencies.add(type, "$group:$artifactid:$depVersion", configureClosure)
        } ?: let {
            dependencies.add(type, "$group:$artifactid:$depVersion")
        }
    }
}

fun Project.getLocalProperties(): Properties {
    val properties = Properties()
    try {
        val localPropertiesFile = File(project.rootDir, "local.properties")
        FileInputStream(localPropertiesFile).use { inputStream ->
            properties.load(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return properties
}


fun Project.getLocalProperty(key: String): String {
    try {
        return getLocalProperties().getProperty(key) ?: ""
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}
