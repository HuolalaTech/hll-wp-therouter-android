package com.therouter.plugin;

import com.android.build.api.variant.*
import com.therouter.plugin.utils.TheRouterPluginUtils
import com.android.build.api.artifact.ScopedArtifact
import com.therouter.plugin.agp8.TextParameters
import com.therouter.plugin.agp8.TheRouterTask
import kotlin.Unit
import kotlin.jvm.functions.Function1;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

import com.android.build.api.instrumentation.InstrumentationScope
import com.therouter.plugin.agp8.TheRouterASM

public class TheRouterPlugin implements Plugin<Project> {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    public void apply(Project project) {
        final TheRouterExtension theRouterExtension = project.getExtensions().create("TheRouter", TheRouterExtension.class);
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library");
        if (!isLibrary) {
            final File cacheFile = new File(project.layout.buildDirectory.get().asFile, "therouter/build.cache");
            if (!cacheFile.exists()) {
                cacheFile.getParentFile().mkdirs()
                cacheFile.createNewFile()
            }
            final File dataFile = new File(project.getProjectDir(), "therouter.data");
            final String buildDataText = TheRouterPluginUtils.getTextFromFile(dataFile);
            cacheFile.write("")

            def android = project.getExtensions().getByType(AndroidComponentsExtension.class);
            android.onVariants(android.selector().all(), new Action<Variant>() {
                @Override
                public void execute(final Variant variant) {
                    variant.getInstrumentation().transformClassesWith(TheRouterASM.class, InstrumentationScope.ALL, new Function1<TextParameters, Unit>() {
                        @Override
                        public Unit invoke(TextParameters textParameters) {
                            textParameters.getBuildDataText().set(buildDataText);
                            textParameters.getBuildCacheFile().set(cacheFile);
                            textParameters.getDebugValue().set(theRouterExtension.debug);
                            return null;
                        }
                    });

                    String variantName = "${variant.name.charAt(0).toUpperCase()}${variant.name.substring(1)}"
                    TaskProvider<TheRouterTask> theRouterTask = project.tasks.register("TheRouter${variantName}", TheRouterTask.class, task -> {
                        task.setTheRouterExtension(theRouterExtension);
                        task.setBuildDataText(buildDataText)
                        task.setBuildCacheFile(cacheFile)
                        task.setBuildDataFile(dataFile)
                    })
                    variant.artifacts
                            .forScope(ScopedArtifacts.Scope.PROJECT)
                            .use(theRouterTask)
                            .toTransform(ScopedArtifact.CLASSES.INSTANCE,
                                    TheRouterTask::getAllJars,
                                    TheRouterTask::getAllDirectories,
                                    TheRouterTask::getOutputFile)
                }
            })
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must be applied in the app module! Remove it from module " + project.getName() + ".");
        }
    }
}