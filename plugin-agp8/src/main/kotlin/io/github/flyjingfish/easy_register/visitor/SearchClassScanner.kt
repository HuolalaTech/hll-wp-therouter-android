package io.github.flyjingfish.easy_register.visitor

import io.github.flyjingfish.easy_register.utils.RegisterClassUtils
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

class SearchClassScanner(private val moduleName:String) : ClassVisitor(Opcodes.ASM9) {
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        RegisterClassUtils.addClass(moduleName,name,superName,interfaces)
    }
}