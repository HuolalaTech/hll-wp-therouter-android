package com.therouter.plugin.agp8;

import com.android.build.api.instrumentation.InstrumentationScope;
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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public abstract class AGP8Plugin implements Plugin<Project> {
    boolean isShow = false;

    public void applyPlugin(final Project project, TheRouterExtension theRouterExtension) {
        AndroidComponentsExtension android = project.getExtensions().getByType(AndroidComponentsExtension.class);
        android.onVariants(android.selector().all(), new Action<Variant>() {
            @Override
            public void execute(final Variant variant) {
                String cachePath = project.getBuildDir().getAbsolutePath();
                if (!theRouterExtension.incrementalCachePath.isBlank()) {
                    cachePath = new File(project.getRootDir(), theRouterExtension.incrementalCachePath).getAbsolutePath();
                }
                final File therouterBuildFolder = new File(cachePath, "therouter");
                boolean isIncremental = theRouterExtension.forceIncremental || (theRouterExtension.debug && therouterBuildFolder.exists());
                if (!isShow) {
                    isShow = true;
                    System.out.println();
                    System.out.println("----------------------TheRouter Environment------------------------------");
                    System.out.println("TheRouter plugin agp8：" + LogUI.C_BLACK_GREEN.getValue() + "cn.therouter:" + BuildConfig.NAME + ":" + BuildConfig.VERSION + LogUI.E_NORMAL.getValue());
                    System.out.println("JDK Version::" + System.getProperty("java.version"));
                    System.out.println("Gradle Version::" + project.getGradle().getGradleVersion());
                    System.out.println("本次是增量构建::" + isIncremental);
                    System.out.println("checkRouteMap::" + theRouterExtension.checkRouteMap);
                    System.out.println("checkFlowDepend::" + theRouterExtension.checkFlowDepend);
                    System.out.println("forceIncremental::" + theRouterExtension.forceIncremental);
                    System.out.println("incrementalCachePath::" + cachePath);

                    if (isIncremental) {
                        System.out.println();
                        System.out.println("本次构建已开启增量编译，可在：\n" + project.getBuildFile().getAbsolutePath() + " 中设置关闭");
                        System.out.println("TheRouter { ");
                        System.out.println("\tdebug = false");
                        System.out.println("\tforceIncremental = false");
                        System.out.println("}");
                    }

                    System.out.println("----------------------TheRouter Environment------------------------------");
                    System.out.println();
                }
                if (isIncremental) {
                    variant.getInstrumentation().transformClassesWith(TheRouterASM.class, InstrumentationScope.ALL, new Function1<TextParameters, Unit>() {
                        @Override
                        public Unit invoke(TextParameters textParameters) {
                            textParameters.getTheRouterBuildFolder().set(therouterBuildFolder);
                            return null;
                        }
                    });
                    String name = "theRouterGetAllClassesWith" + variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
                    TaskProvider<TheRouterGetAllTask> testTask = project.getTasks().register(name, TheRouterGetAllTask.class, task -> {
                        task.setTheRouterExtension(theRouterExtension);
                        task.setTheRouterBuildFolder(therouterBuildFolder);
                    });
                    variant.getArtifacts()
                            .forScope(ScopedArtifacts.Scope.ALL)
                            .use(testTask)
                            .toGet(ScopedArtifact.CLASSES.INSTANCE,
                                    TheRouterGetAllTask::getAllJars,
                                    TheRouterGetAllTask::getAllDirectories);
                    final String compileDebugJavaWithJavac = "compile" + variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1) + "JavaWithJavac";
                    project.afterEvaluate(new Action<Project>() {
                        @Override
                        public void execute(Project p) {
                            p.getTasks().named(compileDebugJavaWithJavac).configure(task ->
                                    task.finalizedBy(testTask)
                            );
                        }
                    });
                } else {
                    therouterBuildFolder.mkdir();
                    String variantName = "theRouterTransformWith" + variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
                    TaskProvider<TheRouterTask> theRouterTask = project.getTasks().register(variantName, TheRouterTask.class, task -> {
                        task.setTheRouterExtension(theRouterExtension);
                        task.setTheRouterBuildFolder(therouterBuildFolder);
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