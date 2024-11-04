package com.therouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.api.variant.*
import com.android.build.api.artifact.ScopedArtifact
import com.therouter.plugin.agp8.TextParameters
import com.therouter.plugin.agp8.TheRouterTask
import kotlin.Unit
import kotlin.jvm.functions.Function1;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

import java.nio.charset.StandardCharsets
import com.android.build.api.instrumentation.InstrumentationScope
import com.therouter.plugin.agp8.TheRouterASM

public class TheRouterPlugin implements Plugin<Project> {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    void apply(Project project) {
        final TheRouterExtension theRouterExtension = project.extensions.create('TheRouter', TheRouterExtension)
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library")
        if (!isLibrary) {
            // 默认开启
            boolean useAGP8 = !"false".equalsIgnoreCase(getGradleProperty(project, "agp8"))
            String gradleVersion = project.gradle.gradleVersion
            int v = gradleVersion.tokenize('.')[0].toInteger()
            if (v < 7 || !useAGP8) {
                def android = project.extensions.getByType(AppExtension)
                def therouterTransform = new TheRouterTransform(project)
                android.registerTransform(therouterTransform)
            } else {
                final File cacheFile = new File(project.layout.buildDirectory.get().asFile, "therouter/build.cache");
                if (!cacheFile.exists()) {
                    cacheFile.getParentFile().mkdirs()
                    cacheFile.createNewFile()
                }
                final File dataFile = new File(project.getProjectDir(), "src/main/assets/therouter/build.data");
                StringBuilder dataStringBuilder = new StringBuilder();
                if (dataFile.exists()) {
                    try {
                        String[] array = dataFile.getText(StandardCharsets.UTF_8.displayName()).split("\n")
                        ArrayList<String> list = new ArrayList<>()
                        for (String item : array) {
                            list.add(item.trim())
                        }
                        Collections.sort(list)
                        for (String item : list) {
                            if (!item.isBlank()) {
                                dataStringBuilder.append(item).append("\n")
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read build.data file", e);
                    }
                }

                final buildDataText = dataStringBuilder.toString();
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