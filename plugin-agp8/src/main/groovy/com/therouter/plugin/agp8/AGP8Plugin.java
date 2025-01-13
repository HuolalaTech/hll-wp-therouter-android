package com.therouter.plugin.agp8;

import com.android.build.api.variant.*;
import com.android.build.api.artifact.ScopedArtifact;
import com.therouter.plugin.BuildConfig;
import com.therouter.plugin.LogUI;
import com.therouter.plugin.TheRouterExtension;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public abstract class AGP8Plugin implements Plugin<Project> {
    public void applyPlugin(Project project, TheRouterExtension theRouterExtension) {
        project.afterEvaluate(p -> {
            System.out.println("----------------------TheRouter Environment------------------------------");
            System.out.println("TheRouter编译插件：" + LogUI.C_BLACK_GREEN.getValue() + "cn.therouter:" + BuildConfig.NAME + ":" + BuildConfig.VERSION + LogUI.E_NORMAL.getValue());
            System.out.println("JDK Version::" + System.getProperty("java.version"));
            System.out.println("Gradle Version::" + project.getGradle().getGradleVersion());
            System.out.println("本次是增量编译::" + theRouterExtension.debug);
            System.out.println("checkRouteMap::" + theRouterExtension.checkRouteMap);
            System.out.println("checkFlowDepend::" + theRouterExtension.checkFlowDepend);
            System.out.println("----------------------TheRouter Environment------------------------------");
        });

        AndroidComponentsExtension android = project.getExtensions().getByType(AndroidComponentsExtension.class);
        android.onVariants(android.selector().all(), new Action<Variant>() {
            @Override
            public void execute(final Variant variant) {
                if (!theRouterExtension.debug) {
                    String variantName = "TheRouter" + variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
                    TaskProvider<TheRouterTask> theRouterTask = project.getTasks().register(variantName, TheRouterTask.class, task -> {
                        task.setTheRouterExtension(theRouterExtension);
                    });
                    variant.getArtifacts()
                            .forScope(ScopedArtifacts.Scope.ALL)
                            .use(theRouterTask)
                            .toTransform(ScopedArtifact.CLASSES.INSTANCE,
                                    TheRouterTask::getAllJars,
                                    TheRouterTask::getAllDirectories,
                                    TheRouterTask::getOutputFile);
                }
            }
        });
    }
}