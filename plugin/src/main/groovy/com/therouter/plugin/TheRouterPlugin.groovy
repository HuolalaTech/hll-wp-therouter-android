package com.therouter.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.therouter.plugin.agp8.TextParameters
import com.therouter.plugin.agp8.TheRouterASM
import com.therouter.plugin.agp8.TheRouterGetAllTask
import com.therouter.plugin.agp8.TheRouterTask
import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

import java.security.MessageDigest

public class TheRouterPlugin implements Plugin<Project> {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";
    boolean isShow = false;

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
                agp8Plugin(project, theRouterExtension)
            }
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must be applied in the app module! Remove it from module " + project.getName() + ".");
        }
    }

    public void agp8Plugin(final Project project, TheRouterExtension theRouterExtension) {
        AndroidComponentsExtension android = project.getExtensions().getByType(AndroidComponentsExtension.class);
        android.onVariants(android.selector().all(), new Action<Variant>() {
            @Override
            public void execute(final Variant variant) {
                if (theRouterExtension.assetRouteMapPath == null || theRouterExtension.assetRouteMapPath.isBlank()) {
                    theRouterExtension.assetRouteMapPath = new File(project.getProjectDir(), "src/main/assets/therouter/routeMap.json").getAbsolutePath();
                }
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
//                    System.out.println(LogUI.C_ERROR.getValue() + "⚠️警告:你的接入方式已经废弃，请将根目录 build.gradle 中【cn.therouter.agp8】替换为【cn.therouter】" + LogUI.E_NORMAL.getValue());
                    System.out.println("TheRouter plugin agp8：" + LogUI.C_BLACK_GREEN.getValue() + "cn.therouter:" + BuildConfig.NAME + ":" + BuildConfig.VERSION + LogUI.E_NORMAL.getValue());
                    System.out.println("JDK Version::" + System.getProperty("java.version"));
                    System.out.println("Gradle Version::" + project.getGradle().getGradleVersion());
                    System.out.println("本次是增量构建::" + isIncremental);
                    System.out.println("checkRouteMap::" + theRouterExtension.checkRouteMap);
                    System.out.println("checkFlowDepend::" + theRouterExtension.checkFlowDepend);
                    System.out.println("forceIncremental::" + theRouterExtension.forceIncremental);
                    System.out.println("incrementalCachePath::" + cachePath);

                    if (theRouterExtension.forceIncremental && theRouterExtension.incrementalCachePath.isBlank()) {
                        System.out.println(LogUI.C_WARN.getValue() + "TheRouter警告：" + LogUI.E_NORMAL.getValue());
                        System.out.println(LogUI.C_WARN.getValue() + "你配置了forceIncremental=true，但未配置incrementalCachePath，这有可能造成prd包运行时执行反射逻辑，建议修改！" + LogUI.E_NORMAL.getValue());
                        System.out.println(LogUI.C_WARN.getValue() + "配置逻辑请见文档：https://therouter.cn/docs/2024/07/22/01" + LogUI.E_NORMAL.getValue());
                    }

                    if (isIncremental) {
                        System.out.println();
                        System.out.println("本次构建已开启增量编译，可在：\n" + project.getBuildFile().getAbsolutePath() + " 中设置关闭");
                        System.out.println("TheRouter { ");
                        System.out.println("\tdebug = false");
                        System.out.println("\tforceIncremental = false");
                        System.out.println("}");
                        System.out.println("详细原理请见文档：https://kymjs.com/code/2024/10/31/01/");
                    }

                    System.out.println("----------------------TheRouter Environment------------------------------");
                    System.out.println();
                }
                if (isIncremental) {
                    variant.getInstrumentation().transformClassesWith(TheRouterASM.class, InstrumentationScope.ALL, new Function1<TextParameters, Unit>() {
                        @Override
                        public Unit invoke(TextParameters textParameters) {
                            textParameters.getTheRouterBuildFolder().set(therouterBuildFolder);
                            textParameters.getCacheContentHash().set(computeCacheHash(therouterBuildFolder));
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

    private static String computeCacheHash(File therouterBuildFolder) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5")
            ["serviceProvide.therouter", "autowired.therouter", "route.therouter"].each { name ->
                File f = new File(therouterBuildFolder, name)
                if (f.exists()) {
                    md.update(f.bytes)
                }
            }
            return md.digest().encodeHex().toString()
        } catch (Exception e) {
            return "empty"
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