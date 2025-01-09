package com.therouter.plugin.agp8;

import com.android.build.api.variant.*;
import com.android.build.api.artifact.ScopedArtifact;
import com.therouter.plugin.TheRouterExtension;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import com.android.build.api.instrumentation.InstrumentationScope;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class AGP8Plugin implements Plugin<Project> {
    public void applyPlugin(Project project, TheRouterExtension theRouterExtension) {
        final File flowTaskFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/flowtask.data");
        final File routeFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/route.data");
        // asm target class
        final File asmTargetFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/asm.data");
        // all class
        final File allClassFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/all.data");
        final File tagFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/tag.data");

        final boolean isFirst = !asmTargetFile.exists();
        if (isFirst) {
            asmTargetFile.getParentFile().mkdirs();
            try {
                tagFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                asmTargetFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (tagFile.exists()) {
                tagFile.delete();
                try {
                    File intermediatesFolder = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "intermediates");
                    deleteDirectory(intermediatesFolder.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!allClassFile.exists()) {
            allClassFile.getParentFile().mkdirs();
            try {
                allClassFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!routeFile.exists()) {
            routeFile.getParentFile().mkdirs();
            try {
                routeFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!flowTaskFile.exists()) {
            flowTaskFile.getParentFile().mkdirs();
            try {
                flowTaskFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        AndroidComponentsExtension android = project.getExtensions().getByType(AndroidComponentsExtension.class);
        android.onVariants(android.selector().all(), new Action<Variant>() {
            @Override
            public void execute(final Variant variant) {
                ScopedArtifacts.Scope scope = ScopedArtifacts.Scope.ALL;
//                if (!isFirst && theRouterExtension.debug) {
//                    scope = ScopedArtifacts.Scope.PROJECT;
//
//                    String tempText = "";
//                    if (TheRouterPluginUtils.needCheckRouteItemClass(theRouterExtension.checkRouteMap)) {
//                        tempText = TheRouterPluginUtils.getTextFromFile(allClassFile);
//                    }
//                    final String allClassText = tempText;
//                    final String asmTargetText = TheRouterPluginUtils.getTextFromFile(asmTargetFile);
//
//                    variant.getInstrumentation().transformClassesWith(TheRouterASM.class, InstrumentationScope.ALL, new Function1<TextParameters, Unit>() {
//                        @Override
//                        public Unit invoke(TextParameters textParameters) {
//                            textParameters.getAsmTargetText().set(asmTargetText);
//                            textParameters.getAllClassText().set(allClassText);
//                            textParameters.getAsmTargetFile().set(asmTargetFile);
//                            textParameters.getAllClassFile().set(allClassFile);
//                            textParameters.getFlowTaskFile().set(flowTaskFile);
//                            textParameters.getRouteFile().set(routeFile);
//                            textParameters.getDebugValue().set(theRouterExtension.debug);
//                            textParameters.getCheckRouteMapValue().set(theRouterExtension.checkRouteMap);
//                            textParameters.getCheckFlowDependValue().set(theRouterExtension.checkFlowDepend);
//                            return null;
//                        }
//                    });
//                }
                String buildTypeName = variant.getBuildType();
                boolean isDebug;
                if (buildTypeName != null){
                    isDebug = "debug".equalsIgnoreCase(buildTypeName);
                }else{
                    String variantName = variant.getName();
                    isDebug = variantName.toLowerCase().contains("debug");
                }

                if (isDebug){
                    try {
                        Class initPluginClass = Class.forName("io.github.flyjingfish.easy_register.plugin.InitPlugin");
                        Method method  = initPluginClass.getDeclaredMethod(
                                "transformClassesWith",
                                Project.class,
                                Variant.class
                        );
                        method.setAccessible(true);
                        // 检查是否是静态方法并调用
                        method.invoke(null, project, variant);
                    } catch (ClassNotFoundException | IllegalAccessException |
                             InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }

                    return;
                }
                String variantName = "TheRouter" + variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
                TaskProvider<TheRouterTask> theRouterTask = project.getTasks().register(variantName, TheRouterTask.class, task -> {
                    task.setTheRouterExtension(theRouterExtension);
                    task.setAsmTargetFile(asmTargetFile);
                    task.setAllClassFile(allClassFile);
                    task.setFlowTaskFile(flowTaskFile);
                    task.setRouteFile(routeFile);
                    task.setFirst(true);
                });
                variant.getArtifacts()
                        .forScope(scope)
                        .use(theRouterTask)
                        .toTransform(ScopedArtifact.CLASSES.INSTANCE,
                                TheRouterTask::getAllJars,
                                TheRouterTask::getAllDirectories,
                                TheRouterTask::getOutputFile);
            }
        });
    }

    public static void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}