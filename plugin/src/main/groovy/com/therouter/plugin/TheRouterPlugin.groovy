package com.therouter.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class TheRouterPlugin implements Plugin<Project> {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    void apply(Project project) {
        project.extensions.create('TheRouter', TheRouterExtension)
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library")
        if (!isLibrary) {
            def android = project.extensions.getByType(AppExtension)
            def therouterTransform = new TheRouterTransform(project)
            android.registerTransform(therouterTransform)
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must call in app module! You need remove at ${project.name} module.")
        }
    }
}