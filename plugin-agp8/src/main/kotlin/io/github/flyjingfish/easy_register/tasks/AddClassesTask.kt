package io.github.flyjingfish.easy_register.tasks

import io.github.flyjingfish.easy_register.utils.AsmUtils
import io.github.flyjingfish.easy_register.utils.checkExist
import io.github.flyjingfish.easy_register.utils.getRelativePath
import io.github.flyjingfish.easy_register.utils.registerCompileTempDir
import io.github.flyjingfish.easy_register.utils.saveEntry
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File


abstract class AddClassesTask : DefaultTask() {
    @get:Input
    abstract var variant :String

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun taskAction() {
        addClass()
    }

    private fun addClass() = runBlocking {
        val tmpOtherDir = File(registerCompileTempDir(project,variant))
        if (tmpOtherDir.exists()){
            tmpOtherDir.deleteRecursively()
        }
        AsmUtils.createInitClass(tmpOtherDir)
        val wovenCodeJobs = mutableListOf<Deferred<Unit>>()
        val needDeleteFiles = mutableListOf<String>()
        val outputDir = File(output.get().asFile.absolutePath)
        for (file in tmpOtherDir.walk()) {
            if (file.isFile) {
                val job = async(Dispatchers.IO) {
                    val relativePath = file.getRelativePath(tmpOtherDir)
                    val target = File(outputDir.absolutePath + File.separatorChar + relativePath)
                    target.checkExist()
                    synchronized(needDeleteFiles){
                        needDeleteFiles.add(target.absolutePath)
                    }
                    file.inputStream().use {
                        target.saveEntry(it)
                    }
                }
                wovenCodeJobs.add(job)
            }
        }
        wovenCodeJobs.awaitAll()
    }

}