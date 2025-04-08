package com.therouter.plugin

import com.android.build.gradle.AppExtension
import com.therouter.plugin.agp8.AGP8Plugin
import org.gradle.api.Project

public class TheRouterPlugin extends AGP8Plugin {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    void apply(Project project) {
        final TheRouterExtension theRouterExtension = project.extensions.create('TheRouter', TheRouterExtension)
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library")
        if (!isLibrary) {
            String gradleVersion = project.gradle.gradleVersion
            int v = gradleVersion.tokenize('.')[0].toInteger()
            if (v < 7) {
                def android = project.extensions.getByType(AppExtension)
                def therouterTransform = new TheRouterTransform(project)
                android.registerTransform(therouterTransform)
            } else {
                super.applyPlugin(project, theRouterExtension)
            }
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must be applied in the app module! Remove it from module " + project.getName() + ".");
        }
    }

    def getGradleProperties(Project project) {
        def properties = new Properties()
        try {
            File localPropertiesFile = project.rootProject.file('gradle.properties')
            properties.load(new FileInputStream(localPropertiesFile))
            return properties
        } catch (Exception e) {
            return properties
        }
    }

    def getGradleProperty(Project project, String key) {
        try {
            return getGradleProperties(project).getProperty(key)
        } catch (Exception e) {
            return ""
        }
    }

}