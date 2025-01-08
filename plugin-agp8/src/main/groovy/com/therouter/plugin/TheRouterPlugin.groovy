package com.therouter.plugin;

import com.therouter.plugin.agp8.AGP8Plugin
import com.therouter.plugin.utils.EasyRegisterJson
import io.github.flyjingfish.easy_register.plugin.InitPlugin
import io.github.flyjingfish.easy_register.utils.RegisterClassUtils
import org.gradle.api.Project

public class TheRouterPlugin extends AGP8Plugin {
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    public void apply(Project project) {
        RegisterClassUtils.INSTANCE.setEnable(true)
        RegisterClassUtils.INSTANCE.setMode("auto")
        InitPlugin.INSTANCE.initFromJson(EasyRegisterJson.jsons)
        InitPlugin.INSTANCE.rootPluginDeepApply(project)

        final TheRouterExtension theRouterExtension = project.getExtensions().create("TheRouter", TheRouterExtension.class);
        boolean isLibrary = project.getPlugins().hasPlugin("com.android.library");
        if (!isLibrary) {
            super.applyPlugin(project, theRouterExtension)
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must be applied in the app module! Remove it from module " + project.getName() + ".");
        }
    }
}