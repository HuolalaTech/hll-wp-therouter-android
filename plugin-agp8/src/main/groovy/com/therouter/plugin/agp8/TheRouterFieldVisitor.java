package com.therouter.plugin.agp8;

import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;

public class TheRouterFieldVisitor extends ClassVisitor {
    private final File file;
    private final boolean debug;

    public TheRouterFieldVisitor(ClassVisitor cv, File name, boolean debug) {
        super(Opcodes.ASM7, cv);
        file = name;
        this.debug = debug;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (TheRouterInjects.FIELD_ROUTER_MAP.equals(name) || TheRouterInjects.FIELD_FLOW_TASK_JSON.equals(name)) {
            if (value instanceof String && !TheRouterPluginUtils.getTextFromFile(file).contains((String) value)) {
                TheRouterPluginUtils.addTextToFileIgnoreCheck(file, value.toString(), debug);
            }
        }
        return super.visitField(access, name, descriptor, signature, value);
    }
}