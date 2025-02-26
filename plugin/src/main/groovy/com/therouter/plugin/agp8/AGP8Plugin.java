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

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AGP8Plugin implements Plugin<Project> {
    public void applyPlugin(final Project project, TheRouterExtension theRouterExtension) {
        final AtomicBoolean existsBuildFolder = new AtomicBoolean(false);
        final File therouterBuildFolder = new File(project.getBuildDir(), "therouter");
        project.afterEvaluate(p -> {
            existsBuildFolder.set(therouterBuildFolder.exists());
            System.out.println();
            System.out.println("----------------------TheRouter Environment------------------------------");
            System.out.println("TheRouter plugin agp8：" + LogUI.C_BLACK_GREEN.getValue() + "cn.therouter:" + BuildConfig.NAME + ":" + BuildConfig.VERSION + LogUI.E_NORMAL.getValue());
            System.out.println("JDK Version::" + System.getProperty("java.version"));
            System.out.println("Gradle Version::" + project.getGradle().getGradleVersion());
            System.out.println("本次是增量构建::" + (theRouterExtension.debug && existsBuildFolder.get()));
            System.out.println("checkRouteMap::" + theRouterExtension.checkRouteMap);
            System.out.println("checkFlowDepend::" + theRouterExtension.checkFlowDepend);

            if (theRouterExtension.debug && existsBuildFolder.get()) {
                System.out.println();
                System.out.println("增量构建不会校验路由表信息，可在：\n" + project.getBuildFile().getAbsolutePath() + " 中如下设置关闭增量编译。");
                System.out.println("TheRouter { ");
                System.out.println("\tdebug = false");
                System.out.println("}");
            }

            System.out.println("----------------------TheRouter Environment------------------------------");
            System.out.println();
        });

        AndroidComponentsExtension android = project.getExtensions().getByType(AndroidComponentsExtension.class);
        android.onVariants(android.selector().all(), new Action<Variant>() {
            @Override
            public void execute(final Variant variant) {
                if (!theRouterExtension.debug || !existsBuildFolder.get()) {
                    therouterBuildFolder.mkdir();
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