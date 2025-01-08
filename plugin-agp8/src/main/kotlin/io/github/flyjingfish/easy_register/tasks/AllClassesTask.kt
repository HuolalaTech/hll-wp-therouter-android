package io.github.flyjingfish.easy_register.tasks

import io.github.flyjingfish.easy_register.utils.RegisterClassUtils
import io.github.flyjingfish.easy_register.utils.AsmUtils
import io.github.flyjingfish.easy_register.utils.computeMD5
import io.github.flyjingfish.easy_register.utils.getFileClassname
import io.github.flyjingfish.easy_register.utils.getRelativePath
import io.github.flyjingfish.easy_register.utils.isJarSignatureRelatedFiles
import io.github.flyjingfish.easy_register.utils.openJar
import io.github.flyjingfish.easy_register.utils.printLog
import io.github.flyjingfish.easy_register.utils.registerCompileTempDir
import io.github.flyjingfish.easy_register.utils.registerTransformIgnoreJarDir
import io.github.flyjingfish.easy_register.utils.slashToDot
import io.github.flyjingfish.easy_register.utils.toClassPath
import io.github.flyjingfish.easy_register.visitor.RegisterClassVisitor
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.system.measureTimeMillis


abstract class AllClassesTask : DefaultTask() {

    @get:Input
    abstract var variant :String

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    private lateinit var jarOutput: JarOutputStream
    private val ignoreJar = mutableSetOf<String>()
    private val ignoreJarClassPaths = mutableListOf<File>()
    @TaskAction
    fun taskAction() {
        printLog("easy-register:release search code start")
        jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))
        val scanTimeCost = measureTimeMillis {
            scanFile()
        }
        jarOutput.close()
        printLog("easy-register:release search code finish, current cost time ${scanTimeCost}ms")

    }


    private fun scanFile() {
        loadJoinPointConfig()
        wovenIntoCode()
    }

    private fun loadJoinPointConfig(){
        val isClassesJar = allDirectories.get().isEmpty() && allJars.get().size == 1
        ignoreJar.clear()
        ignoreJarClassPaths.clear()
        allJars.get().forEach { file ->
            if (isClassesJar){
                ignoreJar.add(file.asFile.absolutePath)
                return@forEach
            }
            val jarFile = JarFile(file.asFile)
            val enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement()
                try {
                    val entryName = jarEntry.name
                    if (entryName.isJarSignatureRelatedFiles()){
                        ignoreJar.add(file.asFile.absolutePath)
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            jarFile.close()
        }
        if (ignoreJar.isNotEmpty()){
            val temporaryDir = File(registerTransformIgnoreJarDir(project,variant))
            for (path in ignoreJar) {
                val destDir = "${temporaryDir.absolutePath}${File.separatorChar}${File(path).name.computeMD5()}"
                val destFile = File(destDir)
                destFile.deleteRecursively()
                openJar(path,destDir)
                ignoreJarClassPaths.add(destFile)
            }
        }

        fun processFile(file : File){
            AsmUtils.processFileForConfig(project,file)
        }
        for (directory in ignoreJarClassPaths) {
            directory.walk().forEach { file ->
                processFile(file)
            }
        }

        //第一遍找配置文件
        allDirectories.get().forEach { directory ->
            directory.asFile.walk().forEach { file ->
                processFile(file)
            }
        }

        allJars.get().forEach { file ->
            if (file.asFile.absolutePath in ignoreJar){
                return@forEach
            }
            AsmUtils.processJarForConfig(file.asFile)
        }
    }

    private fun wovenIntoCode() = runBlocking{

        fun processFile(file : File,directory:File){
            if (file.isFile) {
                val relativePath = file.getRelativePath(directory)
                val jarEntryName: String = relativePath.toClassPath()

                if (isInstrumentable(jarEntryName)){
                    FileInputStream(file).use { inputs ->
                        saveClasses(inputs,jarEntryName,jarOutput)
                    }

                }else{
                    file.inputStream().use {
                        jarOutput.saveEntry(jarEntryName,it)
                    }
                }
            }

        }
        val wovenCodeFileJobs1 = mutableListOf<Deferred<Unit>>()
        for (directory in ignoreJarClassPaths) {
            directory.walk().sortedBy {
                it.name.length
            }.forEach { file ->
                val job = async(Dispatchers.IO) {
                    processFile(file, directory)
                }
                wovenCodeFileJobs1.add(job)
            }

        }
        wovenCodeFileJobs1.awaitAll()
        val wovenCodeFileJobs2 = mutableListOf<Deferred<Unit>>()
        allDirectories.get().forEach { directory ->
            directory.asFile.walk().forEach { file ->
                val job = async(Dispatchers.IO) {
                    processFile(file,directory.asFile)
                }
                wovenCodeFileJobs2.add(job)
            }
        }
        wovenCodeFileJobs2.awaitAll()



        allJars.get().forEach { file ->
            if (file.asFile.absolutePath in ignoreJar){
                return@forEach
            }
            val jarFile = JarFile(file.asFile)
            val enumeration = jarFile.entries()
            val wovenCodeJarJobs = mutableListOf<Deferred<Unit>>()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement()
                val entryName = jarEntry.name
                if (jarEntry.isDirectory || entryName.isEmpty() || entryName.startsWith("META-INF/") || "module-info.class" == entryName) {
                    continue
                }
                val job = async(Dispatchers.IO) {
                    try {
                        if (isInstrumentable(entryName)){
                            jarFile.getInputStream(jarEntry).use { inputs ->
                                saveClasses(inputs,entryName,jarOutput)
                            }
                        }else{
                            jarFile.getInputStream(jarEntry).use {
                                jarOutput.saveEntry(entryName,it)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                wovenCodeJarJobs.add(job)
            }

            wovenCodeJarJobs.awaitAll()
            jarFile.close()
        }
        val tmpOtherDir = File(registerCompileTempDir(project,variant))
        AsmUtils.createInitClass(tmpOtherDir)
        for (file in tmpOtherDir.walk()) {
            if (file.isFile) {
                val className = file.getFileClassname(tmpOtherDir)
                file.inputStream().use {
                    jarOutput.saveEntry(className,it)
                }
            }
        }
    }

    private fun isInstrumentable(className: String): Boolean {
        // 指定哪些类可以被修改，例如过滤某些包名
        return RegisterClassUtils.isWovenClass(slashToDot(className).replace(Regex("\\.class$"), ""))
    }

    fun saveClasses(inputs: InputStream,jarEntryName:String,jarOutput: JarOutputStream){
        val cr = ClassReader(inputs)
        val cw = ClassWriter(cr,0)
        cr.accept(
            RegisterClassVisitor(cw),
            0
        )
        cw.toByteArray().inputStream().use {
            jarOutput.saveEntry(jarEntryName,it)
        }
    }

    fun JarOutputStream.saveEntry(entryName: String, inputStream: InputStream) {
        synchronized(this@AllClassesTask){
            putNextEntry(JarEntry(entryName))
            inputStream.copyTo( this)
            closeEntry()
        }

    }


}