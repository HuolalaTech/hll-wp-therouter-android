package io.github.flyjingfish.easy_register.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import io.github.flyjingfish.easy_register.tasks.AddClassesTask
import io.github.flyjingfish.easy_register.utils.EasyRegisterJson
import io.github.flyjingfish.easy_register.utils.Mode
import io.github.flyjingfish.easy_register.utils.RegisterClassUtils
import io.github.flyjingfish.easy_register.visitor.MyClassVisitorFactory
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized

object InitPlugin{
    private fun deepSetAllModuleSearchCode(project: Project){
        val childProjects = project.childProjects
        if (childProjects.isEmpty()){
            return
        }
        childProjects.forEach { (_,value)->
            value.afterEvaluate {
                val notApp = !it.plugins.hasPlugin(AppPlugin::class.java)
                val noneHasPlugin = !it.plugins.hasPlugin("easy.register")
                if (notApp && noneHasPlugin && it.hasProperty("android")){
                    SearchCodePlugin(true).apply(it)
                }
            }
            deepSetAllModuleSearchCode(value)
        }
    }
    fun rootPluginDeepApply(project: Project) {
        if (project.rootProject == project){
            deepSetAllModuleSearchCode(project.rootProject)
        }
        SearchCodePlugin(false).apply(project)
    }

    fun initFromJson(jsons:List<String>) {
        RegisterClassUtils.initConfig(jsons)
    }

    @JvmStatic
    fun initRoot(project: Project) {
        RegisterClassUtils.enable = true
        RegisterClassUtils.mode = Mode.AUTO
        initFromJson(EasyRegisterJson.jsons)
        rootPluginDeepApply(project)
    }

    @JvmStatic
    fun transformClassesWith(project: Project, variant: Variant) {
        variant.instrumentation.transformClassesWith(
            MyClassVisitorFactory::class.java,
            InstrumentationScope.ALL
        ) { params ->
            params.myConfig.set("My custom config")
        }

        variant.instrumentation.setAsmFramesComputationMode(
            FramesComputationMode.COPY_FRAMES
        )

        val taskProvider = project.tasks.register("${variant.name}EasyRegisterAddClasses",
            AddClassesTask::class.java){
            it.variant = variant.name
        }
        taskProvider.configure {
            it.dependsOn("compile${variant.name.capitalized()}JavaWithJavac")
            it.outputs.upToDateWhen { return@upToDateWhen false }
        }
        variant.artifacts
            .forScope(ScopedArtifacts.Scope.PROJECT)
            .use(taskProvider)
            .toAppend(
                ScopedArtifact.CLASSES,
                AddClassesTask::output
            )
    }


}