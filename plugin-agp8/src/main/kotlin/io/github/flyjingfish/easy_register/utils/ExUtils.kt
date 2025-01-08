package io.github.flyjingfish.easy_register.utils

import org.gradle.api.Project
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.ZipInputStream

fun registerTransformIgnoreJarDir(project:Project, variantName:String):String{
    return project.buildDir.absolutePath+"/tmp/easy-register/${variantName}/tempTransformIgnoreJar/".adapterOSPath()
}

fun registerCompileTempJson(project:Project, variantName:String):String{
    return project.buildDir.absolutePath + "/tmp/easy-register/${variantName}/tempCompileClassJson/needDelClassInfo.json".adapterOSPath()
}

fun registerCompileTempWovenJson(project:Project, variantName:String):String{
    return project.buildDir.absolutePath + "/tmp/easy-register/${variantName}/tempCompileClassJson/needDelWovenClassInfo.json".adapterOSPath()
}
fun registerCompileTempDir(project: Project, variantName:String):String{
    return project.buildDir.absolutePath + "/tmp/easy-register/${variantName}/tempCompileClass/".adapterOSPath()
}

fun hintCleanFile(project: Project):String{
    return project.buildDir.absolutePath + "/tmp/easy-register/hintClean.json".adapterOSPath()
}

fun String.adapterOSPath():String {
    return replace('/', File.separatorChar)
}

fun File.getRelativePath(directory :File):String {
    return directory.toURI().relativize(toURI()).path
}

fun File.checkExist(delete:Boolean = false){
    if (!parentFile.exists()){
        parentFile.mkdirs()
    }
    if (!exists()){
        createNewFile()
    }else if (delete){
        delete()
        createNewFile()
    }
}

fun File.saveEntry(inputStream: InputStream) {
    this.outputStream().use {
        inputStream.copyTo(it)
    }
}

fun ByteArray.saveFile(outFile : File){
    inputStream().use { inputStream->
        outFile.saveEntry(inputStream)
    }
}

fun Int.addPublic(isAddPublic: Boolean):Int{
    return if (isAddPublic){
        if (this and Opcodes.ACC_PUBLIC != 0){
            this
        }else{
            this and (Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).inv() or Opcodes.ACC_PUBLIC
        }
    }else{
        this
    }
}
private fun bytesToHex(bytes: ByteArray): String {
    val hexString = StringBuilder()
    for (b in bytes) {
        val hex = Integer.toHexString(0xff and b.toInt())
        if (hex.length == 1) {
            hexString.append('0')
        }
        hexString.append(hex)
    }
    return hexString.toString()
}
fun String.computeMD5(): String {
    return try {
        val messageDigest = MessageDigest.getInstance("MD5")
        val digestBytes = messageDigest.digest(toByteArray())
        bytesToHex(digestBytes)
    } catch (var3: NoSuchAlgorithmException) {
        throw IllegalStateException(var3)
    }
}

fun dotToSlash(str: String): String {
    return str.replace(".", "/")
}

fun slashToDot(str: String): String {
    return str.replace("/", ".")
}

fun printLog(text: String) {
    if (!RegisterClassUtils.log) return
    println(text)
}

val JAR_SIGNATURE_EXTENSIONS = setOf("SF", "RSA", "DSA", "EC")
fun String.isJarSignatureRelatedFiles(): Boolean {
    return startsWith("META-INF/") && substringAfterLast('.') in JAR_SIGNATURE_EXTENSIONS
}

fun openJar(jarPath:String,destDir:String) {
    // JAR文件路径和目标目录
    ZipInputStream(FileInputStream(jarPath)).use { zis ->
        while (true) {
            val entry = zis.nextEntry ?: break
            val entryName: String = entry.name
            if (entryName.isEmpty() || entryName.startsWith("META-INF/") || "module-info.class" == entryName) {
                continue
            }
            val filePath: String = destDir + File.separator + entryName
            if (!entry.isDirectory) {
                val file = File(filePath)
                val parent = file.parentFile
                if (!parent.exists()) {
                    parent.mkdirs()
                }
                FileOutputStream(file).use {
                    zis.copyTo(it)
                }
            } else {
                File(filePath).mkdirs()
            }
        }
    }
}

fun File.getFileClassname(directory :File):String {
    return getRelativePath(directory).toClassPath()
}

fun String.toClassPath():String {
    return replace(File.separatorChar, '/')
}

fun getWovenClassName(className:String,methodName:String,methodDesc:String):String{
    return className+"\$Woven"+(methodName+ methodDesc).computeMD5()
}
