package com.therouter.plugin.utils;

import org.gradle.api.Project
import com.therouter.plugin.Node

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

    public static void fillTodoList(Map<String, Set<String>> map, String root) {
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

    static Set<String> dependStack = new HashSet<>()

    public static void fillNode(Node node, String root) {
        if (node.children == null || node.children.isEmpty()) {
            if (root == null) {
                dependStack.add(node.name)
            } else {
                dependStack.add(node.name + " --> " + root)
            }
        } else {
            node.children.each {
                if (root == null) {
                    fillNode(it, node.name)
                } else {
                    fillNode(it, node.name + " --> " + root)
                }
            }
        }
    }

    public static Node createNode(Map<String, Set<String>> map, String root) {
        Node node = new Node(root)
        Set<Node> childrenNode = new HashSet<>()
        Set<String> dependsSet = map[root]
        if (dependsSet != null && !dependsSet.isEmpty()) {
            for (depend in dependsSet) {
                childrenNode.add(createNode(map, depend))
            }
        }
        node.children = childrenNode
        return node
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