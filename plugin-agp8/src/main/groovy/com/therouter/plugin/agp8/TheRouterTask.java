package com.therouter.plugin.agp8;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.therouter.plugin.AddCodeVisitor;
import com.therouter.plugin.BuildConfig;
import com.therouter.plugin.JarInfo;
import com.therouter.plugin.LogUI;
import com.therouter.plugin.RouteItem;
import com.therouter.plugin.TheRouterExtension;
import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.TheRouterPlugin;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public abstract class TheRouterTask extends DefaultTask {

    private static final Set<String> allClass = new HashSet<>();
    private TheRouterExtension theRouterExtension;

    @InputFiles
    public abstract ListProperty<RegularFile> getAllJars();

    @InputFiles
    public abstract ListProperty<Directory> getAllDirectories();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public void setTheRouterExtension(TheRouterExtension theRouterExtension) {
        this.theRouterExtension = theRouterExtension;
    }

    @TaskAction
    public void taskAction() throws ClassNotFoundException, IOException {
        System.out.println("TheRouter编译插件：" + LogUI.C_BLACK_GREEN.getValue() + "cn.therouter:" + BuildConfig.NAME + ":" + BuildConfig.VERSION + LogUI.E_NORMAL.getValue());
        System.out.println("JDK Version::" + System.getProperty("java.version"));
        System.out.println("Gradle Version::" + getProject().getGradle().getGradleVersion());
        System.out.println("checkRouteMap::" + theRouterExtension.checkRouteMap);
        System.out.println("checkFlowDepend::" + theRouterExtension.checkFlowDepend);

        System.out.println("----------------------TheRouter build start------------------------------");
        theRouterTransform();
        System.out.println("----------------------TheRouter build finish-----------------------------");
    }

    private void theRouterTransform() throws ClassNotFoundException, IOException {
        File theRouterClassOutputFile = null;
        Set<String> routeMapStringSet = new HashSet<>();
        Map<String, String> flowTaskMap = new HashMap<>();

        File dest = getOutputFile().get().getAsFile();
        System.out.println("========dest:" + dest.getAbsolutePath());
        FileUtils.forceDelete(dest);

        File buildDir = getProject().getLayout().getBuildDirectory().getAsFile().get();
        File theRouterCacheFolder = new File(buildDir, "therouter");
        theRouterCacheFolder.mkdirs();

        for (RegularFile file : getAllJars().get()) {
            File jar = file.getAsFile();
            String jarName = jar.getName().toLowerCase();
            String cacheName = jarName.equals("classes.jar") ? "classes-" + Math.abs(jar.getAbsolutePath().hashCode()) + ".jar" : jarName;
            File cacheFile = new File(theRouterCacheFolder, cacheName);
            JarInfo jarInfo;
            String logInfo = "---------TheRouter handle jar " + jarName + "  ";
            if (cacheFile.exists()) {
                jarInfo = TheRouterInjects.fromCache(cacheFile);
                if (jar.length() != jarInfo.lastModified) {
                    debugLog(logInfo + "CHANGED");
                    cacheFile.delete();
                    jarInfo = TheRouterInjects.tagJar(jar);
                    jarInfo.lastModified = jar.length();
                    TheRouterInjects.toCache(cacheFile, jarInfo);
                } else {
                    debugLog(logInfo + "UP-TO-DATE");
                }
            } else {
                debugLog(logInfo + "EMPTY_CACHE");
                jarInfo = TheRouterInjects.tagJar(jar);
                jarInfo.lastModified = jar.length();
                TheRouterInjects.toCache(cacheFile, jarInfo);
            }
            allClass.addAll(jarInfo.allJarClass);
            if (jarInfo.isTheRouterJar) {
                theRouterClassOutputFile = dest;
                try {
                    FileUtils.copyFile(jar, dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                debugLog("---------TheRouter jar path is " + jar.getAbsolutePath());
            }
        }

        for (Directory directory : getAllDirectories().get()) {
            directory.getAsFileTree().forEach(file -> {
                String relativePath = directory.getAsFile().toURI().relativize(file.toURI()).getPath().replace(File.separatorChar, '/');
                tag(relativePath);
                if (isRouterMap(relativePath)) {
                    try {
                        ClassReader reader = new ClassReader(new FileInputStream(file.getAbsolutePath()));
                        ClassNode cn = new ClassNode();
                        reader.accept(cn, 0);
                        for (FieldNode fieldNode : cn.fields) {
                            if (TheRouterInjects.FIELD_ROUTER_MAP.equals(fieldNode.name)) {
                                System.out.println("---------TheRouter in jar get route map from: " + relativePath + "-------------------------------");
                                routeMapStringSet.add((String) fieldNode.value);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (isServiceProvider(relativePath)) {
                    try {
                        ClassReader reader = new ClassReader(new FileInputStream(file.getAbsolutePath()));
                        ClassNode cn = new ClassNode();
                        reader.accept(cn, 0);
                        for (FieldNode fieldNode : cn.fields) {
                            if (TheRouterInjects.FIELD_FLOW_TASK_JSON.equals(fieldNode.name)) {
                                System.out.println("---------TheRouter in jar get flow task json from: " + relativePath + "-------------------------------");
                                Map<String, String> map = TheRouterInjects.gson.fromJson((String) fieldNode.value, new TypeToken<Map<String, String>>() {
                                }.getType());
                                flowTaskMap.putAll(map);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if (theRouterClassOutputFile != null && theRouterClassOutputFile.exists()) {
            TheRouterInjects.injectClassCode(theRouterClassOutputFile);
            System.out.println("---------TheRouter ASM finish----------------------");
        }

        Set<RouteItem> pageSet = new HashSet<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (String routeMapString : routeMapStringSet) {
            pageSet.addAll(gson.fromJson(routeMapString, new TypeToken<List<RouteItem>>() {
            }.getType()));
        }

        // 让第三方Activity也支持路由，第三方页面的路由表可以在assets中添加
        // 获取项目目录下 assets/therouter/routeMap.json 文件
        File assetRouteMap = new File(getProject().getProjectDir(), "src/main/assets/therouter/routeMap.json");

        // 如果文件存在
        if (assetRouteMap.exists()) {
            // 如果 checkRouteMap 配置为 DELETE
            if (TheRouterPlugin.DELETE.equalsIgnoreCase(theRouterExtension.checkRouteMap)) {
                System.out.println("---------TheRouter delete route map------------------------------------------");
                // 删除并重新创建 routeMap.json
                assetRouteMap.delete();
                try {
                    assetRouteMap.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 读取 JSON 文件内容
                String assetString = null;
                try {
                    assetString = FileUtils.readFileToString(assetRouteMap, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("---------TheRouter get route map from: /assets/therouter/routeMap.json-------");
                try {
                    // 将 JSON 字符串反序列化为 RouteItem 列表
                    List<RouteItem> assetsList = gson.fromJson(assetString, new TypeToken<List<RouteItem>>() {
                    }.getType());
                    if (assetsList == null) {
                        assetsList = new ArrayList<>();
                    }
                    // 如果 assetsList 中的 RouteItem 不在 pageSet 中，则添加到 pageSet
                    for (RouteItem item : assetsList) {
                        if (!pageSet.contains(item)) {
                            pageSet.add(item);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 如果文件不存在，创建父目录和文件
            System.out.println("---------TheRouter route map does not exist: /assets/therouter/routeMap.json-------");
            try {
                assetRouteMap.getParentFile().mkdirs();
                assetRouteMap.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, List<RouteItem>> result = new HashMap<>();

        // 遍历 pageSet 中的每个 routeItem
        for (RouteItem routeItem : pageSet) {
            String url = routeItem.path;

            // 检查 URL 是否包含查询参数（?）
            if (url.contains("?")) {
                try {
                    // 使用 URI 解析路径
                    URI uri = new URI(routeItem.path);
                    // 假设 URI 中有属性方法来获取查询参数（这里的 getProperties 需要替换为具体获取参数的方法）
                    Map<String, String> map = extractQueryParams(uri);

                    // 将查询参数存入 routeItem 的 params 中
                    routeItem.params.putAll(map);

                    // 将 URL 去掉查询参数部分
                    url = url.substring(0, url.indexOf('?'));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            // 获取当前 URL 对应的 RouteItem 列表
            List<RouteItem> routeList = result.get(url);
            if (routeList == null) {
                routeList = new ArrayList<>();
                result.put(url, routeList);
            }

            // 将当前 routeItem 添加到列表中
            routeList.add(routeItem);
        }
        // 检查路由表合法性
        for (List<RouteItem> routeItems : result.values()) {
            String className = null;

            // 遍历每个 RouteItem
            for (RouteItem routeItem : routeItems) {
                if (className == null) {
                    className = routeItem.className;
                } else if (!className.equals(routeItem.className)) {
                    throw new RuntimeException("Multiple Activity to single Url: " + className + " and " + routeItem.className);
                }

                // 检查路由表是否为空
                if (!theRouterExtension.checkRouteMap.isEmpty()) {
                    boolean classNotFound = true;

                    // 遍历 mergeClass 以检查 routeItem.className
                    for (String item : allClass) {
                        // routeItem.className 格式为 com.therouter.demo.shell.TestActivity
                        // item 格式为 com/therouter/demo/shell/TestActivity.class
                        if (item.contains(routeItem.className.replace(".", "/"))) {
                            classNotFound = false;
                            break;
                        }
                    }
                    if (classNotFound) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(theRouterExtension.checkRouteMap)) {
                            throw new ClassNotFoundException(routeItem.className + " in /assets/therouter/routeMap.json");
                        } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(theRouterExtension.checkRouteMap)) {
                            System.out.println(LogUI.C_WARN.getValue() + "[" + routeItem.className + " in /assets/therouter/routeMap.json]" + LogUI.E_NORMAL.getValue());
                        }
                    }
                }
            }
        }
        List<RouteItem> pageList = new ArrayList<>(pageSet);
        Collections.sort(pageList);
        String json = gson.toJson(pageList);
        FileUtils.write(assetRouteMap, json, false);
        System.out.println("---------TheRouter create new route map--------------");

        Map<String, Set<String>> flowTaskDependMap = new HashMap<>();
        for (String key : flowTaskMap.keySet()) {
            Set<String> value = flowTaskDependMap.get(key);

            if (value == null) {
                value = new HashSet<>();
            }

            String dependsOn = flowTaskMap.get(key);
            if (dependsOn != null && !dependsOn.isBlank()) {
                String[] dependencies = dependsOn.split(",");
                for (String depend : dependencies) {
                    if (!depend.isBlank()) {
                        value.add(depend.trim());
                    }
                }
            }

            flowTaskDependMap.put(key, value);
        }

        if (!theRouterExtension.checkFlowDepend.isEmpty()) {
            for (String taskName : flowTaskDependMap.keySet()) {
                Set<String> dependencies = flowTaskDependMap.get(taskName);

                for (String dependency : dependencies) {
                    if (!flowTaskDependMap.containsKey(dependency)) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(theRouterExtension.checkFlowDepend)) {
                            throw new RuntimeException("\n\n==========================================\n" +
                                    "TheRouter:: FlowTask::\n" +
                                    "Can not found Task: [" + dependency + "] from " + taskName + " dependsOn\n" +
                                    "==========================================\n\n");
                        } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(theRouterExtension.checkFlowDepend)) {
                            System.out.println();
                            System.out.println(LogUI.C_WARN.getValue() + "==========================================" + LogUI.E_NORMAL.getValue());
                            System.out.println(LogUI.C_WARN.getValue() + "TheRouter:: FlowTask::   " + LogUI.E_NORMAL.getValue());
                            System.out.println(LogUI.C_WARN.getValue() + "Can not found Task: [" + dependency + "] from " + taskName + " dependsOn" + LogUI.E_NORMAL.getValue());
                            System.out.println(LogUI.C_WARN.getValue() + "==========================================" + LogUI.E_NORMAL.getValue());
                            System.out.println();
                        }
                    }
                }
            }
        }

        // 遍历 flowTaskDependMap 的 keySet 并调用 fillTodoList
        for (String key : flowTaskDependMap.keySet()) {
            TheRouterPluginUtils.fillTodoList(flowTaskDependMap, key);
        }

        if (Boolean.valueOf(theRouterExtension.showFlowDepend)) {
            // 再次遍历 flowTaskDependMap 的 keySet 并调用 fillNode
            for (String key : flowTaskDependMap.keySet()) {
                TheRouterPluginUtils.fillNode(TheRouterPluginUtils.createNode(flowTaskDependMap, key), null);
            }

            System.out.println();
            System.out.println(LogUI.C_WARN.getValue() + "TheRouter:: FlowTask::dependency   " + LogUI.E_NORMAL.getValue());
            System.out.println(LogUI.C_WARN.getValue() + "==========================================" + LogUI.E_NORMAL.getValue());

            // 对 dependStack 排序并打印
            List<String> dependStack = new ArrayList<>(TheRouterPluginUtils.dependStack);
            Collections.sort(dependStack);
            for (String it : dependStack) {
                System.out.println(LogUI.C_WARN.getValue() + "[Root --> " + it + "]" + LogUI.E_NORMAL.getValue());
            }

            System.out.println(LogUI.C_WARN.getValue() + "==========================================" + LogUI.E_NORMAL.getValue());
            System.out.println();
        }
        System.out.println("---------TheRouter check flow task map--------------");
    }

    private void debugLog(String log) {
        if (theRouterExtension.debug) {
            System.out.println(log);
        }
    }

    private static void tag(String className) {
        // a/ServiceProvider__TheRouter__737372.class
        className = className.replaceAll(TheRouterInjects.DOT_CLASS, "");
        if (isAutowired(className)) {
            TheRouterInjects.autowiredSet.add(className);
        } else if (isRouterMap(className)) {
            TheRouterInjects.routeSet.add(className);
        } else if (isServiceProvider(className)) {
            TheRouterInjects.serviceProvideMap.put(className, BuildConfig.VERSION);
        }
    }

    static boolean isAutowired(String className) {
        return className.endsWith(TheRouterInjects.SUFFIX_AUTOWIRED);
    }

    static boolean isRouterMap(String className) {
        return (className.startsWith(TheRouterInjects.PREFIX_ROUTER_MAP)
                || className.startsWith("a/" + TheRouterInjects.PREFIX_ROUTER_MAP))
                && !className.contains("\\$");
    }

    static boolean isServiceProvider(String className) {
        return (className.startsWith(TheRouterInjects.PREFIX_SERVICE_PROVIDER)
                || className.startsWith("a/" + TheRouterInjects.PREFIX_SERVICE_PROVIDER))
                && !className.contains("\\$");
    }

    // 一个辅助方法，用来从 URI 中提取查询参数
    private static Map<String, String> extractQueryParams(URI uri) {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String query = uri.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return queryPairs;
    }
}