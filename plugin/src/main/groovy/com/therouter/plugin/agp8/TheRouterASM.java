package com.therouter.plugin.agp8;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.google.gson.reflect.TypeToken;
import com.therouter.plugin.AddCodeVisitor;
import com.therouter.plugin.BuildConfig;
import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TheRouterASM implements AsmClassVisitorFactory<TextParameters> {
    public static final String INJECTER_FULL_CLASSNAME = "a.TheRouterServiceProvideInjecter";

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor classVisitor) {
        String currentClassName = classContext.getCurrentClassData().getClassName();
        if (INJECTER_FULL_CLASSNAME.equals(currentClassName)) {
            String buildDataText = getParameters().get().getBuildDataText().get();
            String[] classNameArray = buildDataText.split("\n");
            Map<String, String> serviceProvideMap = new HashMap<>();
            Set<String> autowiredSet = new HashSet<>();
            Set<String> routeSet = new HashSet<>();
            for (String name : classNameArray) {
                if (name.contains(TheRouterInjects.PREFIX_ROUTER_MAP)) {
                    routeSet.add(name.trim());
                } else if (name.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)) {
                    serviceProvideMap.put(name.trim().substring(2), BuildConfig.VERSION);
                } else if (name.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
                    autowiredSet.add(name.trim());
                }
            }
            return new AddCodeVisitor(classVisitor, serviceProvideMap, autowiredSet, routeSet, false);
        } else {
            if (currentClassName.contains(TheRouterInjects.PREFIX_ROUTER_MAP)
                    || currentClassName.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)
                    || currentClassName.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
                return new TheRouterFieldVisitor(classVisitor, currentClassName);
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        String className = classData.getClassName().replaceAll("\\.", "/");
        TheRouterInjects.allClass.add(className);
        if (className.contains("$")) {
            return false;
        }
        if (className.contains(TheRouterInjects.PREFIX_ROUTER_MAP)
                || className.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)
                || className.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
            File buildCacheFile = getParameters().get().getBuildCacheFile().get();
            boolean isDebug = getParameters().get().getDebugValue().get();
            TheRouterPluginUtils.addTextToFile(buildCacheFile, className, isDebug);
            // 需要读取路由表或FlowTask
            return className.contains(TheRouterInjects.PREFIX_ROUTER_MAP)
                    || className.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER);
        }
        return INJECTER_FULL_CLASSNAME.equals(classData.getClassName());
    }
}