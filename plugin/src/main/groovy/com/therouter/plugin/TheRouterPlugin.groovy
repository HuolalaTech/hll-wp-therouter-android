package com.therouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

public class TheRouterPlugin implements Plugin<Project> {
    public static final String CHECK_ROUTE_MAP = "CHECK_ROUTE_MAP";
    public static final String CHECK_FLOW_UNKNOW_DEPEND = "CHECK_FLOW_UNKNOW_DEPEND";
    public static final String SHOW_FLOW_DEPEND = "SHOW_FLOW_DEPEND";
    public static final String INCREMENTAL = "THEROUTER_OPEN_INCREMENTAL";
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String DELETE = "delete";

    @Override
    void apply(Project project) {
        boolean isLibrary = false
        project.plugins.each {
            if (it.getClass().name.contains("LibraryPlugin")) {
                isLibrary = true
            }
        }
        if (!isLibrary) {
            def android = project.extensions.getByType(AppExtension)
            def therouterTransform = new TheRouterTransform(project)
            android.registerTransform(therouterTransform)
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must call in Application module")
        }
    }
}