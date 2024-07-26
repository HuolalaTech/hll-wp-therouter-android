package com.therouter.plugin

//import com.android.build.api.artifact.ScopedArtifact
//import com.android.build.api.variant.AndroidComponentsExtension
//import com.android.build.api.variant.ScopedArtifacts
//import com.android.build.api.variant.Variant

import com.android.build.gradle.AppExtension

//import com.therouter.plugin.agp8.TheRouterGetAllClassesTask
//import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
//import org.gradle.api.tasks.TaskProvider

public class TheRouterPlugin implements Plugin<Project> {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    void apply(Project project) {
        project.extensions.create('TheRouter', TheRouterExtension)
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library")
        if (!isLibrary) {
//            boolean useAGP8 = project.TheRouter.agp8
//            String gradleVersion = project.gradle.gradleVersion
//            int v = gradleVersion.tokenize('.')[0].toInteger()
//            if (v < 8 && !useAGP8) {
                def android = project.extensions.getByType(AppExtension)
                def therouterTransform = new TheRouterTransform(project)
                android.registerTransform(therouterTransform)
//            } else {
//                def android = project.extensions.getByType(AndroidComponentsExtension.class)
//                android.onVariants(android.selector().all(), new Action<Variant>() {
//                    @Override
//                    void execute(Variant variant) {
//                        TaskProvider<TheRouterGetAllClassesTask> getAllClassesTask = project.tasks.register("${variant.name}TheRouter", TheRouterGetAllClassesTask.class)
//                        variant.artifacts
//                                .forScope(ScopedArtifacts.Scope.ALL)
//                                .use(getAllClassesTask)
//                                .toTransform(ScopedArtifact.CLASSES.INSTANCE,
//                                        { it.getAllJars() },
//                                        { it.getAllDirectories() },
//                                        { it.getOutputDirectory() })
//                    }
//                })
//            }
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must call in app module! You need remove at ${project.name} module.")
        }
    }
}