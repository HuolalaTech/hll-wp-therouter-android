package com.therouter.apt

import java.io.*
import java.math.BigInteger
import java.security.MessageDigest

/**
 * @author zhangtao
 * @since 2019-02-20
 */
fun writeStringToFile(content: String, file: File) {
    FileOutputStream(file).use { fileOutputStream ->
        fileOutputStream.write(content.toByteArray())
        fileOutputStream.flush()
    }
}

/**
 * 从文件中读取文本
 *
 * @param filePath
 * @return
 */
fun readFile(filePath: File): String? {
    var inputStream: InputStream? = null
    inputStream = try {
        FileInputStream(filePath)
    } catch (e: Exception) {
        throw RuntimeException("FileUtils:readFile---->$filePath not found")
    }
    return inputStream2String(inputStream)
}

/**
 * 输入流转字符串
 *
 * @param inputStream
 * @return 一个流中的字符串
 */
fun inputStream2String(inputStream: InputStream?): String? {
    if (null == inputStream) {
        return null
    }
    var resultSb: StringBuilder? = null
    try {
        val br = BufferedReader(InputStreamReader(inputStream))
        resultSb = StringBuilder()
        var len: String?
        while (null != br.readLine().also { len = it }) {
            resultSb.append(len)
        }
    } catch (ex: Exception) {
    } finally {
        closeIO(inputStream)
    }
    return resultSb?.toString()
}

@Throws(IOException::class)
fun copyFile(inputStream: InputStream, out: OutputStream) {
    val buffer = ByteArray(1024)
    var read: Int
    while (inputStream.read(buffer).also { read = it } != -1) {
        out.write(buffer, 0, read)
    }
}

fun getMD5(file: File): String? {
    if (!file.isFile) {
        return null
    }
    val digest: MessageDigest
    val inputStream: FileInputStream
    val buffer = ByteArray(1024)
    var len: Int
    try {
        digest = MessageDigest.getInstance("MD5")
        inputStream = FileInputStream(file)
        while (inputStream.read(buffer, 0, 1024).also { len = it } != -1) {
            digest.update(buffer, 0, len)
        }
        inputStream.close()
    } catch (e: Exception) {
        return null
    }
    val bigInt = BigInteger(1, digest.digest())
    return bigInt.toString(16)
}

/**
 * 关闭流
 *
 * @param closeables
 */
fun closeIO(vararg closeables: Closeable?) {
    if (closeables.isEmpty()) {
        return
    }
    for (cb in closeables) {
        try {
            if (null == cb) {
                continue
            }
            cb.close()
        } catch (e: IOException) {
            throw RuntimeException("FileUtils", e)
        }
    }
}