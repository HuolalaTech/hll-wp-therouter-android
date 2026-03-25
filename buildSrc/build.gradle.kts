plugins {
    id("java-library")
    kotlin("jvm") version "2.2.10"
    `java-gradle-plugin`
    `maven-publish`
    signing
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

gradlePlugin {
    plugins {
        create("plugin") {
            id = "therouter"
            implementationClass = "com.therouter.plugin.TheRouterPlugin"
        }
    }
}

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

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("com.android.tools.build:gradle-api:9.1.0")
    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")
    implementation("com.google.code.gson:gson:2.9.1")
}