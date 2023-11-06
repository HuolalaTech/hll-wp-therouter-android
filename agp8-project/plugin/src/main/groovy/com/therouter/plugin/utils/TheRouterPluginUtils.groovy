package com.therouter.plugin.utils

import com.therouter.plugin.TheRouterPlugin;
import org.gradle.api.Project
import com.therouter.plugin.Node

public class TheRouterPluginUtils {

    private static final Map<String, String> buildProperties = new HashMap<>()

    public static def getLocalProperty(Project project, String key) {
        try {
            if (!buildProperties.containsKey(key)) {
                initProperties()
            }
            def value = buildProperties.get(key)
            return value == null ? "" : value
        } catch (Exception e) {
            e.printStackTrace()
            return ""
        }
    }

    def initProperties() {
        File gradlePropertiesFile
        try {
            gradlePropertiesFile = new File(mProject.rootDir, 'gradle.properties');
            if (gradlePropertiesFile == null || !gradlePropertiesFile.exists()) {
                gradlePropertiesFile = new File("../gradle.properties")
            }
        } catch (Exception e) {
            gradlePropertiesFile = new File("../gradle.properties")
        }
        def gradleProperties = new Properties()
        try {
            gradleProperties.load(new FileInputStream(gradlePropertiesFile))
        } catch (Exception e) {
            e.printStackTrace()
        }
        buildProperties.put(TheRouterPlugin.CHECK_ROUTE_MAP, gradleProperties.getProperty(TheRouterPlugin.CHECK_ROUTE_MAP))
        buildProperties.put(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND, gradleProperties.getProperty(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))
        buildProperties.put(TheRouterPlugin.SHOW_FLOW_DEPEND, gradleProperties.getProperty(TheRouterPlugin.SHOW_FLOW_DEPEND))
        buildProperties.put(TheRouterPlugin.INCREMENTAL, gradleProperties.getProperty(TheRouterPlugin.INCREMENTAL))

        File localPropertiesFile
        try {
            localPropertiesFile = new File(mProject.rootDir, 'local.properties');
            if (localPropertiesFile == null || !localPropertiesFile.exists()) {
                localPropertiesFile = new File("../local.properties")
            }
        } catch (Exception e) {
            localPropertiesFile = new File("../local.properties")
        }
        def localProperties = new Properties()
        try {
            localProperties.load(new FileInputStream(localPropertiesFile))
        } catch (Exception e) {
            e.printStackTrace()
        }
        def v = localProperties.getProperty(TheRouterPlugin.CHECK_ROUTE_MAP)
        if (v != null && v.length() > 0) {
            buildProperties.put(TheRouterPlugin.CHECK_ROUTE_MAP, v)
        }
        v = localProperties.getProperty(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND)
        if (v != null && v.length() > 0) {
            buildProperties.put(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND, v)
        }
        v = localProperties.getProperty(TheRouterPlugin.SHOW_FLOW_DEPEND)
        if (v != null && v.length() > 0) {
            buildProperties.put(TheRouterPlugin.SHOW_FLOW_DEPEND, v)
        }
        v = localProperties.getProperty(TheRouterPlugin.INCREMENTAL)
        if (v != null && v.length() > 0) {
            buildProperties.put(TheRouterPlugin.INCREMENTAL, v)
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