package com.therouter.plugin.utils;

import com.therouter.plugin.Node;
import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.TheRouterPlugin;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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

    public static Set<String> getSetFromFile(File buildCacheFile) {
        HashSet<String> set = new HashSet<>();
        if (buildCacheFile.exists()) {
            try {
                String[] array = ResourceGroovyMethods.getText(buildCacheFile, StandardCharsets.UTF_8.displayName()).split("\n");
                for (String item : array) {
                    if (!item.trim().isBlank()) {
                        set.add(item.trim());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + buildCacheFile.getName() + " file", e);
            }
        }
        return set;
    }

    public static String getTextFromFile(File buildCacheFile) {
        StringBuilder dataStringBuilder = new StringBuilder();
        if (buildCacheFile.exists()) {
            try {
                String[] array = ResourceGroovyMethods.getText(buildCacheFile, StandardCharsets.UTF_8.displayName()).split("\n");
                HashSet<String> set = new HashSet<>();
                for (String item : array) {
                    if (!item.trim().isBlank()) {
                        set.add(item.trim());
                    }
                }
                ArrayList<String> list = new ArrayList<>(set);
                Collections.sort(list);
                for (String item : list) {
                    dataStringBuilder.append(item).append("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + buildCacheFile.getName() + " file", e);
            }
        }
        return dataStringBuilder.toString();
    }

    public static void addTextToFileIgnoreCheck(File buildCacheFile, String line) {
        try {
            ResourceGroovyMethods.append(buildCacheFile, line + "\n", StandardCharsets.UTF_8.displayName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addTextToFile(File buildCacheFile, String line, boolean debug) {
        if (!line.contains("$")) {
            // 从ASMFactory来的是不带.class的，从toTransform来的是带的，还要考虑json的情况，route.data/spi.data都是json
            if (!line.endsWith(TheRouterInjects.DOT_CLASS) && !line.contains("\"") && !line.contains("[") && !line.contains("{")) {
                line = line + TheRouterInjects.DOT_CLASS;
            }
            if (debug) {
                System.out.println("TheRouter::" + buildCacheFile.getName() + " -> " + line);
            }
            try {
                ResourceGroovyMethods.append(buildCacheFile, line + "\n", StandardCharsets.UTF_8.displayName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean needTagClass(String mode) {
        return !mode.isEmpty() && !TheRouterPlugin.DELETE.equals(mode);
    }
}
