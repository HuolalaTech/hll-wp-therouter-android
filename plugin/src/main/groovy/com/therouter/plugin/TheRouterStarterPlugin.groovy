package com.therouter.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

public class TheRouterStarterPlugin implements Plugin<Project> {
    def KAPT = "kapt"
    def IMPLEMENTATION = "implementation"

    @Override
    void apply(Project proj) {
        def isRoot = proj == proj.rootProject
        def isApp = proj.plugins.hasPlugin(AppPlugin)
        def isLib = proj.plugins.hasPlugin(LibraryPlugin)
        def version = proj.rootProject.buildscript.configurations.classpath.dependencies.find {
            it.group == "cn.therouter" && it.name == "plugin"
        }?.version

        if (isRoot) {
            proj.with {

                subprojects {

                    project.plugins.configureEach { Plugin plugin ->
                        switch (plugin.getClass()) {
                            case AppPlugin.class:
                            case LibraryPlugin.class:
                                project.configurations.configureEach { Configuration conf ->
                                    def confName = conf.name
                                    if (confName == KAPT) {
                                        project.dependencies.add(confName, "cn.therouter:apt:$version")
                                    } else if (confName == IMPLEMENTATION) {
                                        project.dependencies.add(confName, "cn.therouter:router:$version")
                                    }
                                }
                                break
                        }
                    }

                }

            }
        } else if (isApp || isLib) {
            proj.with {
                configurations.configureEach { Configuration conf ->
                    def confName = conf.name
                    if (confName == KAPT) {
                        project.dependencies.add(confName, "cn.therouter:apt:$version")
                    } else if (confName == IMPLEMENTATION) {
                        project.dependencies.add(confName, "cn.therouter:router:$version")
                    }
                }
            }
        } else {
            throw new RuntimeException("`apply plugin: 'therouter-starter'` must call in rootProject or Android module")
        }
    }
}