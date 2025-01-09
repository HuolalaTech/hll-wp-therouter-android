package com.therouter.plugin


import com.therouter.plugin.agp8.AGP8Plugin

import org.gradle.api.Project

import java.lang.reflect.Method

public class TheRouterPlugin extends AGP8Plugin {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    public void apply(Project project) {
        Class initPluginClass = Class.forName("io.github.flyjingfish.easy_register.plugin.InitPlugin");
        Method method  = initPluginClass.getDeclaredMethod(
                "initRoot",
                Project.class
        );
        method.setAccessible(true)
        // 检查是否是静态方法并调用
        method.invoke(null, project)

        final TheRouterExtension theRouterExtension = project.getExtensions().create("TheRouter", TheRouterExtension.class);
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library");
        if (!isLibrary) {
            super.applyPlugin(project, theRouterExtension)
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must be applied in the app module! Remove it from module " + project.getName() + ".");
        }
    }
}