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

    @Override
    void apply(Project project) {
        def isApp = project.plugins.hasPlugin(AppPlugin)
        if (isApp) {
            def android = project.extensions.getByType(AppExtension)
            def therouterTransform = new TheRouterTransform(project);
            android.registerTransform(therouterTransform);
        } else {
            throw new RuntimeException("`apply plugin: 'therouter'` must call in Application module")
        }
    }
}