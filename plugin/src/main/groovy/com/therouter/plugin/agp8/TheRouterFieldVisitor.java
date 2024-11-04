package com.therouter.plugin.agp8;

import com.google.gson.reflect.TypeToken;
import com.therouter.plugin.TheRouterInjects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class TheRouterFieldVisitor extends ClassVisitor {
    private String className;

    public TheRouterFieldVisitor(ClassVisitor cv, String name) {
        super(Opcodes.ASM7, cv);
        className = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (TheRouterInjects.FIELD_ROUTER_MAP.equals(name)) {
            if (value instanceof String) {
                System.out.println("---------TheRouter in jar get route map from: " + className + "-------------------------------");
                TheRouterInjects.routeMapStringSet.add(value.toString());
            }
        } else if (TheRouterInjects.FIELD_FLOW_TASK_JSON.equals(name)) {
            if (value instanceof String) {
                System.out.println("---------TheRouter in jar get flow task json from: " + className + "-------------------------------");
                Map<String, String> map = TheRouterInjects.gson.fromJson(value.toString(), new TypeToken<Map<String, String>>() {
                }.getType());
                TheRouterInjects.flowTaskMap.putAll(map);
            }
        }
        return super.visitField(access, name, descriptor, signature, value);
    }
}