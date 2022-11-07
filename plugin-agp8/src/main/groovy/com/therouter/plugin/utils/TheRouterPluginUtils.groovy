package com.therouter.plugin.utils;

import org.gradle.api.Project

public class TheRouterPluginUtils {
    public static def getLocalProperty(Project project, String key) {
        try {
            def value = getLocalProperties(project).getProperty(key)
            return value == null ? "" : value
        } catch (Exception e) {
            e.printStackTrace()
            return ""
        }
    }

    public static def getLocalProperties(Project project) {
        def properties = new Properties()
        try {
            File localPropertiesFile
            try {
                localPropertiesFile = new File(project.rootDir, 'local.properties');
                if (localPropertiesFile == null || !localPropertiesFile.exists()) {
                    localPropertiesFile = new File("../local.properties")
                }
            } catch (Exception e) {
                localPropertiesFile = new File("../local.properties")
            }
            properties.load(new FileInputStream(localPropertiesFile))
            return properties
        } catch (Exception e) {
            e.printStackTrace()
            return properties
        }
    }


    private static final List<String> loopDependStack = new ArrayList<>()

    static void fillTodoList(Map<String, Set<String>> map, String root) {
        Set<String> dependsSet = map.get(root)
        if (dependsSet != null && !dependsSet.isEmpty()) {
            if (loopDependStack.contains(root)) {
                throw new RuntimeException("\n\n==========================================" +
                        "\nTheRouter:: FlowTask::   " +
                        "\nCyclic dependency: [${getLog(loopDependStack, root)}]" +
                        "\n==========================================\n\n")
            }
            loopDependStack.add(root)
            for (depend in dependsSet) {
                fillTodoList(map, depend)
            }
            loopDependStack.remove(root)
        }
    }

    static String getLog(List<String> list, String root) {
        if (list == null || list.isEmpty()) {
            return ""
        }
        StringBuilder stringBuilder = new StringBuilder()
        for (task in list) {
            stringBuilder.append(task).append("-->")
        }
        if (root != null) {
            stringBuilder.append(root)
        }
        return stringBuilder.toString()
    }
}