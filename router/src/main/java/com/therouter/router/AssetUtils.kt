package com.therouter.router

import android.content.Context
import com.therouter.debug
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * @author zhangtao
 * @since 2019-01-28
 */
/**
 * 从assets 文件夹中获取文件并读取数据流
 *
 * @param fileName 文件名
 */
fun getStreamFromAssets(context: Context?, fileName: String?): InputStream? {
    return fileName?.let { context?.resources?.assets?.open(fileName) }
}

/**
 * 从assets 文件夹中获取文件并读取数据
 */
fun getStringFromAssets(context: Context, fileName: String?): String {
    try {
        InputStreamReader(context.resources.assets.open(fileName!!)).use { inputReader ->
            BufferedReader(inputReader).use { bufReader ->
                var line: String?
                val result = StringBuilder()
                while (bufReader.readLine().also { line = it } != null) {
                    result.append(line)
                }
                return result.toString()
            }
        }
    } catch (e: Exception) {
        debug("AssetsUtils", "error read $fileName from assets") {
            e.printStackTrace()
        }
    }
    return ""
}

fun getFileListFromAssets(context: Context?, path: String?): Array<String>? {
    if (context == null) {
        return null
    }
    var files: Array<String>? = null
    val assetManager = context.assets
    try {
        files = assetManager.list(path!!)
    } catch (e: IOException) {
        debug("AssetsUtils", "error read $path from assets") {
            e.printStackTrace()
        }
    }
    return files
}