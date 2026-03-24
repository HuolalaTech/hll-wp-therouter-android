plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ksp)
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



dependencies {
    implementation(libs.gson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp)
    implementation(libs.google.auto.service)
    ksp(libs.zacsweers.auto.service)
}
