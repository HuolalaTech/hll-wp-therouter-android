package com.therouter.plugin.agp8;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.therouter.plugin.AddCodeVisitor;
import com.therouter.plugin.utils.ClassCacheUtils;

import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public abstract class TheRouterASM implements AsmClassVisitorFactory<TextParameters> {
    public static final String INJECTER_FULL_CLASSNAME = "a.TheRouterServiceProvideInjecter";

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor classVisitor) {
        File therouterBuildFolder = getParameters().get().getTheRouterBuildFolder().get();
        try {
            Map<String, String> serviceProvideMap = ClassCacheUtils.readToMap(new File(therouterBuildFolder, "serviceProvide.therouter"));
            Set<String> autowiredSet = ClassCacheUtils.readToSet(new File(therouterBuildFolder, "autowired.therouter"));
            Set<String> routeSet = ClassCacheUtils.readToSet(new File(therouterBuildFolder, "route.therouter"));
            return new AddCodeVisitor(classVisitor, serviceProvideMap, autowiredSet, routeSet, false);
        } catch (IOException e) {
            return classVisitor;
        }
    }

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return INJECTER_FULL_CLASSNAME.equals(classData.getClassName());
    }
}