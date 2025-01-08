package io.github.flyjingfish.easy_register.utils

import io.github.flyjingfish.easy_register.bean.WovenClass
import io.github.flyjingfish.easy_register.tasks.SearchRegisterClassesTask
import io.github.flyjingfish.easy_register.visitor.RegisterClassVisitor
import io.github.flyjingfish.easy_register.visitor.SearchClassScanner
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method
import java.io.File
import java.io.FileInputStream
import java.util.jar.JarFile

object AsmUtils {
    fun processFileForConfig(project: Project, file: File) {
        if (file.isFile) {
            if (file.absolutePath.endsWith(SearchRegisterClassesTask._CLASS)) {
                FileInputStream(file).use { inputs ->
                    val bytes = inputs.readAllBytes()
                    if (bytes.isNotEmpty()) {
                        val classReader = ClassReader(bytes)
                        classReader.accept(
                            SearchClassScanner(project.name),
                            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                        )
                    }
                }
            }

        }
    }

    fun processJarForConfig(file: File) {
        val jarFile = JarFile(file)
        val enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            val jarEntry = enumeration.nextElement()
            try {
                val entryName = jarEntry.name
                if (jarEntry.isDirectory || jarEntry.name.isEmpty()) {
                    continue
                }
                if (entryName.endsWith(SearchRegisterClassesTask._CLASS)) {
                    jarFile.getInputStream(jarEntry).use { inputs ->
                        val bytes = inputs.readAllBytes()
                        if (bytes.isNotEmpty()) {
                            val classReader = ClassReader(bytes)
                            classReader.accept(
                                SearchClassScanner(file.absolutePath),
                                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        jarFile.close()
    }

    fun createInitClass(output:File) {
//        val wovenCodeJobs = mutableListOf<Deferred<Unit>>()
        val list: List<WovenClass> = RegisterClassUtils.getClasses()
        for (wovenClass in list) {
//            val job = async(Dispatchers.IO) {
                val method = Method.getMethod(wovenClass.wovenMethod)
                val searchClass = wovenClass.searchClass

                if (wovenClass.createWovenClass){
                    val className = dotToSlash(wovenClass.wovenClass)
                    val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                    cw.visit(
                        Opcodes.V1_8,
                        Opcodes.ACC_PUBLIC, dotToSlash(className), null, "java/lang/Object", null)

                    var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
                    mv.visitVarInsn(Opcodes.ALOAD, 0)
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    mv.visitInsn(Opcodes.RETURN)
                    mv.visitMaxs(0, 0) //更新操作数栈
                    mv.visitEnd() //一定要有visitEnd


                    mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, method.name, method.descriptor, null, null)

                    val argTypes = Type.getArgumentTypes(method.descriptor)
                    for ((index,_) in argTypes.withIndex()) {
                        mv.visitVarInsn(Opcodes.ALOAD, index)
                    }
                    mv.visitMethodInsn(
                        AdviceAdapter.INVOKESTATIC,
                        getWovenClassName(className,method.name, method.descriptor),
                        RegisterClassVisitor.INVOKE_METHOD,
                        method.descriptor,
                        false
                    )

                    mv.visitInsn(Opcodes.RETURN)
                    mv.visitMaxs(0,0)
                    mv.visitEnd()
                    val path = output.absolutePath + File.separatorChar + dotToSlash(className).adapterOSPath()+".class"
                    val classByteData = cw.toByteArray()
                    val outFile = File(path)
                    outFile.checkExist()
                    classByteData.saveFile(outFile)
                }

                val className = getWovenClassName(dotToSlash(wovenClass.wovenClass),method.name,method.descriptor)
                val path = output.absolutePath + File.separatorChar + dotToSlash(className).adapterOSPath()+".class"
                val outFile = File(path)
                val argTypes = Type.getArgumentTypes(method.descriptor)
                fun addCode(mv:MethodVisitor){
                    val set = searchClass.getClassNames()
                    if (set.isNotEmpty()) {
                        for (routeModuleClassName in set) {
                            if (searchClass.callType == "callee" && searchClass.callClass.isNotEmpty()){
                                val callClazz = dotToSlash(searchClass.callClass)
                                var callMethod : Method ?= null
                                val isMuchMethod = searchClass.callMethod.contains("#")
                                var muchCallClazz = callClazz
                                var callVirtual = false
                                if (isMuchMethod){
                                    val callMethods = searchClass.callMethod.split("#")
                                    for ((index,callMethodStr) in callMethods.withIndex()) {
                                        val method1 = Method.getMethod(callMethodStr)
                                        if (index < callMethods.size - 1){
                                            if (method1.descriptor.startsWith("()") && method1.descriptor != "()V"){
                                                if (index == 0){
                                                    mv.visitMethodInsn(
                                                        AdviceAdapter.INVOKESTATIC,
                                                        muchCallClazz,
                                                        method1.name,
                                                        method1.descriptor,
                                                        false
                                                    )
                                                }else{
                                                    mv.visitMethodInsn(
                                                        AdviceAdapter.INVOKEVIRTUAL,
                                                        muchCallClazz,
                                                        method1.name,
                                                        method1.descriptor,
                                                        false
                                                    )
                                                }
                                                callVirtual = true
                                                muchCallClazz = dotToSlash(method1.returnType.className)
                                            }else{
                                                throw IllegalArgumentException("不支持 $searchClass 的 callMethod")
                                            }
                                        }else{
                                            callMethod = method1
                                        }


                                    }
                                }else{
                                    callMethod = Method.getMethod(searchClass.callMethod)
                                }
                                if (callMethod == null) continue

//                            println("muchCallClazz=$muchCallClazz, callMethod=$callMethod")

                                val callValues = searchClass.callMethodValue.split(",")
                                var containValue = false
                                for (callValue in callValues) {
                                    when (callValue) {
                                        "searchClass" -> {
                                            when (searchClass.useType) {
                                                "className" -> {
                                                    mv.visitLdcInsn(slashToDot(routeModuleClassName))
                                                    containValue = true
                                                }
                                                "new" -> {
                                                    mv.visitTypeInsn(AdviceAdapter.NEW, routeModuleClassName)
                                                    mv.visitInsn(AdviceAdapter.DUP)
                                                    mv.visitMethodInsn(
                                                        AdviceAdapter.INVOKESPECIAL,
                                                        routeModuleClassName,
                                                        "<init>",
                                                        "()V",
                                                        false
                                                    )
                                                    containValue = true
                                                }
                                                "class" -> {
                                                    mv.visitLdcInsn(Type.getObjectType(routeModuleClassName))
                                                    containValue = true
                                                }
                                            }
                                        }
                                        else -> {
                                            val CALL_VALUE_REGEX = Regex("\\$(\\d+)")
                                            val matchResult = CALL_VALUE_REGEX.find(callValue)
                                            if (matchResult != null) {
                                                val number = matchResult.groupValues[1].toInt()
                                                mv.visitVarInsn(Opcodes.ALOAD, number)
//                                            println("muchCallClazz=$muchCallClazz, callMethod=$callMethod，number=$number")
                                                containValue = true
                                            }


                                        }
                                    }
                                }
                                mv.visitMethodInsn(
                                    if (callVirtual) AdviceAdapter.INVOKEVIRTUAL else AdviceAdapter.INVOKESTATIC,
                                    muchCallClazz,
                                    callMethod.name,
                                    callMethod.descriptor,
                                    false
                                )

                            }

                            if (searchClass.callType == "caller"){
                                val callClazz = dotToSlash(routeModuleClassName)
                                var callMethod : Method ?= null
                                val isMuchMethod = searchClass.callMethod.contains("#")
                                var muchCallClazz = callClazz
                                var callVirtual = false
                                if (isMuchMethod){
                                    val callMethods = searchClass.callMethod.split("#")
                                    for ((index,callMethodStr) in callMethods.withIndex()) {
                                        val method1 = Method.getMethod(callMethodStr)
                                        if (index < callMethods.size - 1){
                                            if (method1.descriptor.startsWith("()") && method1.descriptor != "()V"){
                                                if (index == 0){
                                                    mv.visitMethodInsn(
                                                        AdviceAdapter.INVOKESTATIC,
                                                        muchCallClazz,
                                                        method1.name,
                                                        method1.descriptor,
                                                        false
                                                    )
                                                }else{
                                                    mv.visitMethodInsn(
                                                        AdviceAdapter.INVOKEVIRTUAL,
                                                        muchCallClazz,
                                                        method1.name,
                                                        method1.descriptor,
                                                        false
                                                    )
                                                }
                                                callVirtual = true
                                                muchCallClazz = dotToSlash(method1.returnType.className)
                                            }else{
                                                throw IllegalArgumentException("不支持 $searchClass 的 callMethod")
                                            }
                                        }else{
                                            callMethod = method1
                                        }
                                    }
                                }else{
                                    callMethod = Method.getMethod(searchClass.callMethod)
                                }
                                if (callMethod == null) continue

                                val callValues = searchClass.callMethodValue.split(",")
                                var containValue = false
                                for (callValue in callValues) {
                                    when (callValue) {
                                        "searchClass" -> {
                                            when (searchClass.useType) {
                                                "className" -> {
                                                    mv.visitLdcInsn(slashToDot(routeModuleClassName))
                                                    containValue = true
                                                }
                                                "new" -> {
                                                    mv.visitTypeInsn(AdviceAdapter.NEW, routeModuleClassName)
                                                    mv.visitInsn(AdviceAdapter.DUP)
                                                    mv.visitMethodInsn(
                                                        AdviceAdapter.INVOKESPECIAL,
                                                        routeModuleClassName,
                                                        "<init>",
                                                        "()V",
                                                        false
                                                    )
                                                    containValue = true
                                                }
                                                "class" -> {
                                                    mv.visitLdcInsn(Type.getObjectType(routeModuleClassName))
                                                    containValue = true
                                                }
                                            }
                                        }
                                        else -> {
                                            val CALL_VALUE_REGEX = Regex("\\$(\\d+)")
                                            val matchResult = CALL_VALUE_REGEX.find(callValue)
                                            if (matchResult != null) {
                                                val number = matchResult.groupValues[1].toInt()
                                                mv.visitVarInsn(Opcodes.ALOAD, number)
//                                            println("muchCallClazz=$muchCallClazz, callMethod=$callMethod，number=$number")
                                                containValue = true
                                            }
                                        }
                                    }
                                }
//                            println("muchCallClazz=$muchCallClazz, callMethod=$callMethod，callVirtual=$callVirtual")
                                mv.visitMethodInsn(
                                    if (callVirtual) AdviceAdapter.INVOKEVIRTUAL else AdviceAdapter.INVOKESTATIC,
                                    muchCallClazz,
                                    callMethod.name,
                                    callMethod.descriptor,
                                    false
                                )
                            }

                        }
                    }

                }
                if (outFile.exists()){
                    var hasInit = false
                    val cr = ClassReader(outFile.readBytes())
                    val cw = ClassWriter(cr,0)
                    cr.accept(
                        object : ClassVisitor(Opcodes.ASM9, cw){
                            override fun visitMethod(
                                access: Int,
                                name: String,
                                descriptor: String,
                                signature: String?,
                                exceptions: Array<out String>?
                            ): MethodVisitor {
                                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                                if (name == "init" && descriptor == method.descriptor){
                                    hasInit = true
                                    return MyMethodAdapter(mv, access, name, descriptor)
                                }

                                return mv
                            }

                            inner class MyMethodAdapter(mv: MethodVisitor, access: Int, mName: String, mDesc: String) :
                                AdviceAdapter(Opcodes.ASM9, mv, access, mName, mDesc) {

                                override fun visitInsn(opcode: Int) {
                                    if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                                        addCode(mv)
                                    }
                                    super.visitInsn(opcode)
                                }
                            }
                                                               },
                        0
                    )
//                    return@async
                    if (hasInit){
                        cw.toByteArray().saveFile(outFile)

                        continue
                    }

                }
                val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                cw.visit(
                    Opcodes.V1_8,
                    Opcodes.ACC_PUBLIC, dotToSlash(className), null, "java/lang/Object", null)

                var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(0, 0) //更新操作数栈
                mv.visitEnd() //一定要有visitEnd


                mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "init", method.descriptor, null, null)
                mv.visitCode()
                addCode(mv)

                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(argTypes.size, argTypes.size+1)
                mv.visitEnd()

                val classByteData = cw.toByteArray()

                outFile.checkExist()
                classByteData.saveFile(outFile)
//            }
//            wovenCodeJobs.add(job)
        }
//        wovenCodeJobs.awaitAll()

    }


}