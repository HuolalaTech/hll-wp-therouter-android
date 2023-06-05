package com.therouter.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.google.gson.Gson
import com.therouter.plugin.agp8.TheRouterGetAllClassesTask
import com.therouter.plugin.utils.TheRouterPluginUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.tasks.TaskProvider

class TheRouterPlugin implements Plugin<Project> {
    public static final String CHECK_ROUTE_MAP = "CHECK_ROUTE_MAP";
    public static final String CHECK_FLOW_UNKNOW_DEPEND = "CHECK_FLOW_UNKNOW_DEPEND";
    public static final String SHOW_FLOW_DEPEND = "SHOW_FLOW_DEPEND";
    public static final String INCREMENTAL = "THEROUTER_OPEN_INCREMENTAL";
    public static final String WARNING = "warning";
    public static final String ERROR = "error";

    public static final PREFIX_SERVICE_PROVIDER = "ServiceProvider__TheRouter__"
    public static final PREFIX_ROUTER_MAP = "RouterMap__TheRouter__"
    public static final SUFFIX_AUTOWIRED = "__TheRouter__Autowired"
    public static final DOT_CLASS = ".class"
    public static final PREFIX_PACKAGE_DOT = "a."

    public static final FIELD_FLOW_TASK_JSON = "FLOW_TASK_JSON"
    public static final FIELD_APT_VERSION = "THEROUTER_APT_VERSION"
    public static final FIELD_ROUTER_MAP = "ROUTERMAP"
    public static final NOT_FOUND_VERSION = "0.0.0"

    public static final Gson gson = new Gson()

    public static final Map<String, String> serviceProvideMap = new HashMap<>()
    public static final Set<String> autowiredSet = new HashSet<>()
    public static final Set<String> routeSet = new HashSet<>()

    public static Project mProject = null

    @Override
    void apply(Project project) {
        mProject = project
        def isLibrary = project.plugins.hasPlugin("com.android.library")
        if (!isLibrary) {
            println("欢迎使用 TheRouter 编译插件：${LogUI.C_BLACK_GREEN.value}" + "cn.therouter:${BuildConfig.NAME}:${BuildConfig.VERSION}" + "${LogUI.E_NORMAL.value}")
            println "当前编译 JDK Version 为::" + System.getProperty("java.version")
            println "GradleVersion::${project.gradle.gradleVersion}"
            println "CHECK_ROUTE_MAP::${TheRouterPluginUtils.getLocalProperty(project, CHECK_ROUTE_MAP)}"
            println "CHECK_FLOW_UNKNOW_DEPEND::${TheRouterPluginUtils.getLocalProperty(project, CHECK_FLOW_UNKNOW_DEPEND)}"

            if (project.gradle.gradleVersion >= "8") {
                def android = project.extensions.getByType(AndroidComponentsExtension.class)
                android.onVariants(android.selector().all(), new Action<Variant>() {
                    @Override
                    void execute(Variant variant) {
                        TaskProvider<TheRouterGetAllClassesTask> getAllClassesTask = project.tasks.register("${variant.name}TheRouterGetAllClasses", TheRouterGetAllClassesTask.class)
                        variant.artifacts
                                .forScope(ScopedArtifacts.Scope.ALL)
                                .use(getAllClassesTask)
                                .toTransform(ScopedArtifact.CLASSES.INSTANCE, { it.getAllJars() }, { it.getAllDirectories() }, { it.getOutput() })
                    }
                })
            } else {
                def android = project.extensions.getByType(AppExtension)
                def therouterTransform = new TheRouterTransform(project)
                android.registerTransform(therouterTransform)
            }
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must call in Application module")
        }
    }
}
