package com.therouter.plugin.agp8;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.therouter.plugin.AddCodeVisitor;
import com.therouter.plugin.BuildConfig;
import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import org.objectweb.asm.ClassVisitor;

import java.io.File;
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
            File asmTargetFile = getParameters().get().getAsmTargetFile().get();
            Map<String, String> serviceProvideMap = new HashMap<>();
            Set<String> autowiredSet = new HashSet<>();
            Set<String> routeSet = new HashSet<>();
            for (String name : TheRouterPluginUtils.getSetFromFile(asmTargetFile)) {
                if (name.contains(TheRouterInjects.PREFIX_ROUTER_MAP)) {
                    routeSet.add(name.trim());
                } else if (name.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)) {
                    serviceProvideMap.put(name.trim().substring(2), BuildConfig.VERSION);
                } else if (name.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
                    autowiredSet.add(name.trim());
                }
            }
            return new AddCodeVisitor(classVisitor, serviceProvideMap, autowiredSet, routeSet, false);
        } else if (currentClassName.contains(TheRouterInjects.PREFIX_ROUTER_MAP)) {
            return new TheRouterFieldVisitor(classVisitor, getParameters().get().getRouteFile().get());
        } else if (currentClassName.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)) {
            return new TheRouterFieldVisitor(classVisitor, getParameters().get().getFlowTaskFile().get());
        } else {
            return null;
        }
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        String className = classData.getClassName().replaceAll("\\.", "/");
        String allClassText = getParameters().get().getAllClassText().get();
        boolean isDebug = getParameters().get().getDebugValue().get();
        String checkRouteMap = getParameters().get().getCheckRouteMapValue().get();
        if (!allClassText.contains(className) && TheRouterPluginUtils.needTagClass(checkRouteMap)) {
            File allClassFile = getParameters().get().getAllClassFile().get();
            TheRouterPluginUtils.addTextToFile(allClassFile, className, isDebug);
        }
        if (className.contains("$")) {
            return false;
        }
        String asmTargetText = getParameters().get().getAsmTargetText().get();
        if (className.contains(TheRouterInjects.PREFIX_ROUTER_MAP)) {
            if (!asmTargetText.contains(className)) {
                File asmTargetFile = getParameters().get().getAsmTargetFile().get();
                TheRouterPluginUtils.addTextToFile(asmTargetFile, className, isDebug);
            }
            // 需要读取路由表字段
            return TheRouterPluginUtils.needTagClass(checkRouteMap);
        } else if (className.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)) {
            if (!asmTargetText.contains(className)) {
                File asmTargetFile = getParameters().get().getAsmTargetFile().get();
                TheRouterPluginUtils.addTextToFile(asmTargetFile, className, isDebug);
            }
            String checkflowDepend = getParameters().get().getCheckFlowDependValue().get();
            // 需要读取 flow task 字段
            return !checkflowDepend.isEmpty();
        } else if (className.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
            if (!asmTargetText.contains(className)) {
                File asmTargetFile = getParameters().get().getAsmTargetFile().get();
                TheRouterPluginUtils.addTextToFile(asmTargetFile, className, isDebug);
            }
            return false;
        }
        return INJECTER_FULL_CLASSNAME.equals(classData.getClassName());
    }
}