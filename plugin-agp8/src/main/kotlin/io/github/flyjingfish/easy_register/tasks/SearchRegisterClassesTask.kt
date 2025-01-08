package io.github.flyjingfish.easy_register.tasks

import io.github.flyjingfish.easy_register.utils.RegisterClassUtils
import io.github.flyjingfish.easy_register.utils.AsmUtils
import io.github.flyjingfish.easy_register.utils.printLog
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import java.io.File
import kotlin.system.measureTimeMillis

class SearchRegisterClassesTask(
    private val allJars: MutableList<File>,
    private val allDirectories: MutableList<File>,
    private val output: File,
    private val project: Project,
    private val isApp:Boolean,
    private val variantName:String,
    private val isJava:Boolean = true
) {
    companion object{
        const val _CLASS = ".class"

    }

    fun taskAction() {
        printLog("easy-register:debug search code start")
        val scanTimeCost = measureTimeMillis {
            scanFile()
        }
        printLog("easy-register:debug search code finish, current cost time ${scanTimeCost}ms")

    }

    private fun scanFile() = runBlocking {
        searchJoinPointLocation()
    }



    private fun searchJoinPointLocation(){
        //第一遍找配置文件
        RegisterClassUtils.clear(project.name)
        allDirectories.forEach { directory ->
            directory.walk().forEach { file ->
                AsmUtils.processFileForConfig(project,file)
            }

        }

        allJars.forEach { file ->
            RegisterClassUtils.clear(file.absolutePath)
            AsmUtils.processJarForConfig(file)
        }
    }





}