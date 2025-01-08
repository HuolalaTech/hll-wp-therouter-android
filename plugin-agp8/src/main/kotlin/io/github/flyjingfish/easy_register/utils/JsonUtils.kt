package io.github.flyjingfish.easy_register.utils

import com.android.build.gradle.internal.coverage.JacocoReportTask
import io.github.flyjingfish.easy_register.bean.NeedDelFileJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.flyjingfish.easy_register.bean.WovenClass
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap


object JsonUtils {
    private val gson: Gson = GsonBuilder().create()
    fun <T> optFromJsonString(jsonString: String, clazz: Class<T>): T? {
        try {
            return gson.fromJson(jsonString, clazz)
        } catch (e: Throwable) {
            JacocoReportTask.JacocoReportWorkerAction.logger.warn(
                "optFromJsonString(${jsonString}, $clazz",
                e
            )
        }
        return null
    }

    private fun optToJsonString(any: Any): String {
        try {
            return gson.toJson(any)
        } catch (throwable: Throwable) {
            JacocoReportTask.JacocoReportWorkerAction.logger.warn(
                "optToJsonString(${any}",
                throwable
            )
        }
        return ""
    }

    private fun readAsString(path: String): String {
        return try {
            val content = String(Files.readAllBytes(Paths.get(path)));
            content
        } catch (exception: Exception) {
            ""
        }
    }

    fun getJson(files:List<File>):List<WovenClass>{
        val listType: Type = object : TypeToken<List<WovenClass>>() {}.type
        val searchClassList= mutableListOf<WovenClass>()
        for (file in files) {
            val list: List<WovenClass> = gson.fromJson(readAsString(file.absolutePath), listType)
            searchClassList.addAll(list)
        }
        for (wovenClass in searchClassList) {
            if (wovenClass.searchClass.classNames == null){
                wovenClass.searchClass.classNames = ConcurrentHashMap()
            }
        }
        return searchClassList
    }

    fun getJson4Str(jsons:List<String>):List<WovenClass>{
        val listType: Type = object : TypeToken<List<WovenClass>>() {}.type
        val searchClassList= mutableListOf<WovenClass>()
        for (json in jsons) {
            val list: List<WovenClass> = gson.fromJson(json, listType)
            searchClassList.addAll(list)
        }
        for (wovenClass in searchClassList) {
            if (wovenClass.searchClass.classNames == null){
                wovenClass.searchClass.classNames = ConcurrentHashMap()
            }
        }
        return searchClassList
    }

    fun exportConfigJson(project: Project) {
        val jsonFile = File(hintCleanFile(project))
        jsonFile.checkExist()
        saveFile(jsonFile, "")
    }
    private fun saveFile(file: File, data: String) {
        val fos = FileOutputStream(file.absolutePath)
        try {
            fos.write(data.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace();
        } finally {
            fos.close()
        }
    }

    fun exportCacheFile(jsonFile: File, mutableList: MutableList<String>) {
        synchronized(mutableList){
            jsonFile.checkExist()
            val json = GsonBuilder().setPrettyPrinting().create().toJson(NeedDelFileJson(mutableList))
            saveFile(jsonFile, json)
        }

    }

    fun deleteNeedDelFile(project:Project, variantName:String){
        val json : NeedDelFileJson? = optFromJsonString(
            readAsString(registerCompileTempJson(project,variantName)),
            NeedDelFileJson::class.java)
        json?.let {
            it.cacheFileJson.forEach {filePath ->
                val file = File(filePath)
                if (file.exists()){
                    file.delete()
                }
            }
        }
    }

    fun deleteNeedDelWovenFile(project:Project, variantName:String){
        val json : NeedDelFileJson? = optFromJsonString(
            readAsString(registerCompileTempWovenJson(project,variantName)),
            NeedDelFileJson::class.java)
        json?.let {
            it.cacheFileJson.forEach {filePath ->
                val file = File(filePath)
                if (file.exists()){
                    file.delete()
                }
            }
        }
    }
}