package com.therouter.plugin.utils;

import com.therouter.plugin.Node;
import com.therouter.plugin.TheRouterPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TheRouterPluginUtils {

    public static String getLog(List<String> list, String root) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String task : list) {
            stringBuilder.append(task).append("-->");
        }
        if (root != null) {
            stringBuilder.append(root);
        }
        return stringBuilder.toString();
    }

    private static final List<String> loopDependStack = new ArrayList<>();

    public static void fillTodoList(Map<String, Set<String>> map, String root) {
        Set<String> dependsSet = map.get(root);
        if (dependsSet != null && !dependsSet.isEmpty()) {
            if (loopDependStack.contains(root)) {
                throw new RuntimeException("\n\n==========================================" +
                        "\nTheRouter:: FlowTask::   " +
                        "\nCyclic dependency: [" + getLog(loopDependStack, root) + "]" +
                        "\n==========================================\n\n");
            }
            loopDependStack.add(root);
            for (String depend : dependsSet) {
                fillTodoList(map, depend);
            }
            loopDependStack.remove(root);
        }
    }

    public static Set<String> dependStack = new HashSet<>();

    public static void fillNode(Node node, String root) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            if (root == null) {
                dependStack.add(node.getName());
            } else {
                dependStack.add(node.getName() + " --> " + root);
            }
        } else {
            for (Node it : node.getChildren()) {
                if (root == null) {
                    fillNode(it, node.getName());
                } else {
                    fillNode(it, node.getName() + " --> " + root);
                }
            }
        }
    }

    public static Node createNode(Map<String, Set<String>> map, String root) {
        final Node node = new Node(root);
        Set<Node> childrenNode = new HashSet<>();
        Set<String> dependsSet = map.get(root);
        if (dependsSet != null && !dependsSet.isEmpty()) {
            for (String depend : dependsSet) {
                childrenNode.add(createNode(map, depend));
            }
        }
        node.setChildren(childrenNode);
        return node;
    }

    public static boolean needCheckRouteItemClass(String mode) {
        return !mode.isEmpty() && !TheRouterPlugin.DELETE.equals(mode);
    }
}
