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

public abstract class AGP8Plugin implements Plugin<Project> {
    public void applyPlugin(Project project, TheRouterExtension theRouterExtension) {
        final File flowTaskFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/flowtask.data");
        final File routeFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/route.data");
        // asm target class
        final File asmTargetFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/asm.data");
        // all class
        final File allClassFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "therouter/all.data");

        final boolean isFirst = !asmTargetFile.exists();
        if (isFirst) {
            if (asmTargetFile.exists()) {
                asmTargetFile.delete();
            }
            asmTargetFile.getParentFile().mkdirs();
            try {
                asmTargetFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        AndroidComponentsExtension android = project.getExtensions().getByType(AndroidComponentsExtension.class);
        android.onVariants(android.selector().all(), new Action<Variant>() {
            @Override
            public void execute(final Variant variant) {
                if (!theRouterExtension.checkRouteMap.isEmpty()) {
                    if (!allClassFile.exists()) {
                        allClassFile.getParentFile().mkdirs();
                        try {
                            allClassFile.createNewFile();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                ScopedArtifacts.Scope scope = ScopedArtifacts.Scope.ALL;
                if (!isFirst) {
                    scope = ScopedArtifacts.Scope.PROJECT;

                    String tempText = "";
                    if (!theRouterExtension.checkRouteMap.isEmpty()) {
                        tempText = TheRouterPluginUtils.getTextFromFile(allClassFile);
                    }
                    final String allClassText = tempText;
                    final String asmTargetText = TheRouterPluginUtils.getTextFromFile(asmTargetFile);

                    variant.getInstrumentation().transformClassesWith(TheRouterASM.class, InstrumentationScope.ALL, new Function1<TextParameters, Unit>() {
                        @Override
                        public Unit invoke(TextParameters textParameters) {
                            textParameters.getAsmTargetText().set(asmTargetText);
                            textParameters.getAllClassText().set(allClassText);
                            textParameters.getAsmTargetFile().set(asmTargetFile);
                            textParameters.getAllClassFile().set(allClassFile);
                            textParameters.getFlowTaskFile().set(flowTaskFile);
                            textParameters.getRouteFile().set(routeFile);
                            textParameters.getDebugValue().set(theRouterExtension.debug);
                            textParameters.getCheckRouteMapValue().set(theRouterExtension.checkRouteMap);
                            textParameters.getCheckFlowDependValue().set(theRouterExtension.checkFlowDepend);
                            return null;
                        }
                    });
                }

                String variantName = "TheRouter" + variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
                TaskProvider<TheRouterTask> theRouterTask = project.getTasks().register(variantName, TheRouterTask.class, task -> {
                    task.setTheRouterExtension(theRouterExtension);
                    task.setAsmTargetFile(asmTargetFile);
                    task.setAllClassFile(allClassFile);
                    task.setFlowTaskFile(flowTaskFile);
                    task.setRouteFile(routeFile);
                    task.setFirst(isFirst);
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
}