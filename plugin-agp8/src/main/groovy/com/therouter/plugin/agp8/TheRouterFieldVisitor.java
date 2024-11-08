package com.therouter.plugin.agp8;

import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;

public class TheRouterFieldVisitor extends ClassVisitor {
    private final File file;

    public TheRouterFieldVisitor(ClassVisitor cv, File name) {
        super(Opcodes.ASM7, cv);
        file = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (TheRouterInjects.FIELD_ROUTER_MAP.equals(name) || TheRouterInjects.FIELD_FLOW_TASK_JSON.equals(name)) {
            if (value instanceof String) {
                TheRouterPluginUtils.addTextToFileIgnoreCheck(file, value.toString());
            }
        }
        return super.visitField(access, name, descriptor, signature, value);
    }
}