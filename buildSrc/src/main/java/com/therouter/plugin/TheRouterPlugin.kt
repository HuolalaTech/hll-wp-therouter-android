package com.therouter.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.therouter.plugin.agp8.TextParameters
import com.therouter.plugin.agp8.TheRouterASM
import com.therouter.plugin.agp8.TheRouterGetAllTask
import com.therouter.plugin.agp8.TheRouterTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class TheRouterPlugin : Plugin<Project> {
    private var isShow = false

    override fun apply(project: Project) {
        val theRouterExtension = project.extensions.create("TheRouter", TheRouterExtension::class.java)
        val isLibrary = project.plugins.hasPlugin("com.android.library")
        if (!isLibrary) {
            agp8Plugin(project, theRouterExtension)
        } else {
            throw RuntimeException("`apply plugin: 'therouter'` must be applied in the app module! Remove it from module " + project.getName() + ".")
        }
    }

    fun agp8Plugin(project: Project, theRouterExtension: TheRouterExtension) {
        project.extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
            // 处理 assetRouteMapPath
            if (theRouterExtension.assetRouteMapPath == null || theRouterExtension.assetRouteMapPath.isBlank()) {
                val path = File(project.projectDir, "src/main/assets/therouter/routeMap.json").absolutePath
                theRouterExtension.assetRouteMapPath = path
            }
            var cachePath = project.getBuildDir().absolutePath
            if (theRouterExtension.incrementalCachePath != null && !theRouterExtension.incrementalCachePath.isBlank()) {
                cachePath = File(project.rootDir, theRouterExtension.incrementalCachePath).absolutePath
            }
            val therouterBuildFolder = File(cachePath, "therouter")
            val isIncremental =
                theRouterExtension.forceIncremental || (theRouterExtension.debug && therouterBuildFolder.exists())

            if (!isShow) {
                isShow = true
                println()
                println("----------------------TheRouter Environment------------------------------")
//                println(LogUI.C_ERROR.getValue() + "⚠️警告:你的接入方式已经废弃，请将根目录 build.gradle 中【cn.therouter.agp8】替换为【cn.therouter】" + LogUI.E_NORMAL.getValue());
                println("TheRouter plugin agp9：" + LogUI.C_BLACK_GREEN.getValue() + "cn.therouter:" + BuildConfig.NAME + ":" + BuildConfig.VERSION + LogUI.E_NORMAL.getValue())
                println("JDK Version::" + System.getProperty("java.version"))
                println("Gradle Version::" + project.getGradle().getGradleVersion())
                println("本次是增量构建::" + isIncremental)
                println("checkRouteMap::" + theRouterExtension.checkRouteMap)
                println("checkFlowDepend::" + theRouterExtension.checkFlowDepend)
                println("forceIncremental::" + theRouterExtension.forceIncremental)
                println("incrementalCachePath::" + cachePath)

                if (theRouterExtension.forceIncremental &&
                    (theRouterExtension.incrementalCachePath == null || theRouterExtension.incrementalCachePath.isBlank())
                ) {
                    println(LogUI.C_WARN.getValue() + "TheRouter警告：" + LogUI.E_NORMAL.getValue())
                    println(LogUI.C_WARN.getValue() + "你配置了forceIncremental=true，但未配置incrementalCachePath，这有可能造成prd包运行时执行反射逻辑，建议修改！" + LogUI.E_NORMAL.getValue())
                    println(LogUI.C_WARN.getValue() + "配置逻辑请见文档：https://therouter.cn/docs/2024/07/22/01" + LogUI.E_NORMAL.getValue())
                }

                if (isIncremental) {
                    println()
                    println(
                        "本次构建已开启增量编译，可在：\n" + project.getBuildFile().getAbsolutePath() + " 中设置关闭"
                    )
                    println("TheRouter { ")
                    println("\tdebug = false")
                    println("\tforceIncremental = false")
                    println("}")
                    println("详细原理请见文档：https://kymjs.com/code/2024/10/31/01/")
                }

                println("----------------------TheRouter Environment------------------------------")
                println()
            }

            if (isIncremental) {
                // 增量编译：使用 ASM 类转换 + TheRouterGetAllTask
                variant.instrumentation.transformClassesWith(
                    TheRouterASM::class.java,
                    InstrumentationScope.ALL
                ) { textParameters: TextParameters ->
                    textParameters.getTheRouterBuildFolder().set(therouterBuildFolder)
                    textParameters.getCacheContentHash().set(computeCacheHash(therouterBuildFolder))
                }

                val name = "theRouterGetAllClassesWith" +
                        variant.name.substring(0, 1).uppercase(Locale.getDefault()) +
                        variant.name.substring(1)
                val testTask = project.getTasks().register(
                    name,
                    TheRouterGetAllTask::class.java,
                    Action { task: TheRouterGetAllTask? ->
                        task!!.setTheRouterExtension(theRouterExtension)
                        task.setTheRouterBuildFolder(therouterBuildFolder)
                    }
                )

                variant.artifacts
                    .forScope(ScopedArtifacts.Scope.ALL)
                    .use(testTask)
                    .toGet(
                        ScopedArtifact.CLASSES,
                        { obj: TheRouterGetAllTask? -> obj!!.getAllJars() },
                        { obj: TheRouterGetAllTask? -> obj!!.getAllDirectories() }
                    )

                val compileTaskName = "compile" +
                        variant.name.substring(0, 1).uppercase(Locale.getDefault()) +
                        variant.name.substring(1) +
                        "JavaWithJavac"
                project.afterEvaluate(Action { p: Project? ->
                    p!!.getTasks().named(compileTaskName)
                        .configure(Action { task: Task? -> task!!.finalizedBy(testTask) })
                })
            } else {
                // 非增量：使用 TheRouterTask 进行全量转换
                therouterBuildFolder.mkdir()
                val variantName = "theRouterTransformWith" +
                        variant.name.substring(0, 1).uppercase(Locale.getDefault()) +
                        variant.name.substring(1)
                val theRouterTask = project.tasks.register(
                    variantName,
                    TheRouterTask::class.java,
                    Action { task: TheRouterTask ->
                        task.setTheRouterExtension(theRouterExtension)
                        task.setTheRouterBuildFolder(therouterBuildFolder)
                    }
                )

                variant.artifacts
                    .forScope(ScopedArtifacts.Scope.ALL)
                    .use(theRouterTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        { obj: TheRouterTask? -> obj!!.getAllJars() },
                        { obj: TheRouterTask? -> obj!!.getAllDirectories() },
                        { obj: TheRouterTask? -> obj!!.getOutputFile() }
                    )
            }
        }
    }

    companion object {
        const val WARNING: String = "warning"
        const val ERROR: String = "error"
        const val DELETE: String = "delete"
        private fun computeCacheHash(therouterBuildFolder: File?): String {
            try {
                val md = MessageDigest.getInstance("MD5")
                val fileNames = arrayOf<String?>("serviceProvide.therouter", "autowired.therouter", "route.therouter")
                for (name in fileNames) {
                    val f = File(therouterBuildFolder, name)
                    if (f.exists()) {
                        val bytes = f.readBytes()
                        md.update(bytes)
                    }
                }
                val digest = md.digest()
                return bytesToHex(digest)
            } catch (e: Exception) {
                return "empty"
            }
        }

        private fun bytesToHex(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b.toInt() and 0xff))
            }
            return sb.toString()
        }
    }
}