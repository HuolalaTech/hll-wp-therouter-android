package com.therouter.plugin.agp8;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.therouter.plugin.BuildConfig;
import com.therouter.plugin.LogUI;
import com.therouter.plugin.RouteItem;
import com.therouter.plugin.TheRouterExtension;
import com.therouter.plugin.TheRouterInjects;
import com.therouter.plugin.TheRouterPlugin;
import com.therouter.plugin.utils.ClassCacheUtils;
import com.therouter.plugin.utils.TheRouterFlowTask;
import com.therouter.plugin.utils.TheRouterPluginUtils;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class TheRouterGetAllTask extends DefaultTask {

    protected TheRouterExtension theRouterExtension;
    protected File therouterBuildFolder;
    // 让第三方Activity也支持路由，第三方页面的路由表可以在assets中添加
    // 获取项目目录下 assets/therouter/routeMap.json 文件
    protected File assetRouteMap;

    @InputFiles
    public abstract ListProperty<RegularFile> getAllJars();

    @InputFiles
    public abstract ListProperty<Directory> getAllDirectories();

    public void setTheRouterExtension(TheRouterExtension theRouterExtension) {
        this.theRouterExtension = theRouterExtension;
    }

    public void setAssetRouteMapFile(File assetRouteMapFile) {
        this.assetRouteMap = assetRouteMapFile;
    }

    public void setTheRouterBuildFolder(File therouterBuildFolder) {
        this.therouterBuildFolder = therouterBuildFolder;
    }

    @TaskAction
    public void taskAction() throws ClassNotFoundException, IOException {
        System.out.println("----------------------TheRouter build start------------------------------");
        TheRouterInjects.serviceProvideMap.clear();
        TheRouterInjects.autowiredSet.clear();
        TheRouterInjects.routeSet.clear();
        TheRouterInjects.routeMapStringSet.clear();
        TheRouterInjects.flowTaskMap.clear();
        TheRouterInjects.allClass.clear();
        theRouterTransform();
        System.out.println("----------------------TheRouter build finish-----------------------------");
    }

    public void theRouterTransform() throws ClassNotFoundException, IOException {
        File theRouterJar = null;
        JarEntry theRouterServiceProvideInjecter = null;

        Set<String> addedEntries = new HashSet<>();
        for (RegularFile file : getAllJars().get()) {
            File jar = file.getAsFile();
            if (jar.exists()) {
                try {
                    JarFile jarFile = new JarFile(jar);
                    for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                        JarEntry jarEntry = e.nextElement();
                        String name = jarEntry.getName();
                        if (name.contains("META-INF/") || !addedEntries.add(name)
                                || this.theRouterExtension.removeClass.contains(name)) {
                            // 如果已添加该条目，则跳过
                            if (this.theRouterExtension.debug){
                                System.out.println("TheRouter plugin remove file: " + name);
                            }
                            continue;
                        }

                        TheRouterInjects.allClass.add(name);
                        if (name.contains("TheRouterServiceProvideInjecter")) {
                            theRouterJar = jar;
                            theRouterServiceProvideInjecter = jarEntry;
                        } else {
                            if (!name.contains("$")) {
                                if (name.contains(TheRouterInjects.PREFIX_ROUTER_MAP)) {
                                    TheRouterInjects.routeSet.add(name.replaceAll(".class", ""));
                                    ClassReader reader = new ClassReader(jarFile.getInputStream(jarEntry));
                                    ClassNode cn = new ClassNode();
                                    reader.accept(cn, 0);
                                    Map<String, String> fieldMap = new HashMap<>();
                                    int count = 0;
                                    List<FieldNode> fieldList = cn.fields;
                                    for (FieldNode fieldNode : fieldList) {
                                        if (TheRouterInjects.FIELD_ROUTER_MAP_COUNT.equals(fieldNode.name)) {
                                            count = Integer.parseInt(fieldNode.value.toString());
                                        }
                                        if (fieldNode.name.startsWith(TheRouterInjects.FIELD_ROUTER_MAP)) {
                                            fieldMap.put(fieldNode.name, fieldNode.value.toString());
                                        }
                                    }

                                    if (fieldMap.size() == 1 && count == 0) {  // old version
                                        TheRouterInjects.routeMapStringSet.addAll(fieldMap.values());
                                    } else if (fieldMap.size() == count) { // new version
                                        StringBuilder stringBuilder = new StringBuilder();
                                        for (int i = 0; i < count; i++) {
                                            stringBuilder.append(fieldMap.get(TheRouterInjects.FIELD_ROUTER_MAP + i));
                                        }
                                        TheRouterInjects.routeMapStringSet.add(stringBuilder.toString());
                                    }
                                } else if (name.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)) {
                                    TheRouterInjects.serviceProvideMap.put(name.replaceAll(".class", ""), BuildConfig.VERSION);
                                    if (!theRouterExtension.checkFlowDepend.isEmpty()) {
                                        ClassReader reader = new ClassReader(jarFile.getInputStream(jarEntry));
                                        ClassNode cn = new ClassNode();
                                        reader.accept(cn, 0);
                                        List<FieldNode> fieldList = cn.fields;
                                        for (FieldNode fieldNode : fieldList) {
                                            if (TheRouterInjects.FIELD_FLOW_TASK_JSON.equals(fieldNode.name)) {
                                                Map<String, String> map = TheRouterInjects.gson.fromJson(fieldNode.value.toString(), HashMap.class);
                                                TheRouterInjects.flowTaskMap.putAll(map);
                                            }
                                        }
                                    }
                                } else if (name.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
                                    TheRouterInjects.autowiredSet.add(name.replaceAll(".class", ""));
                                }
                            }
                            mergeClassTransform(jarFile.getInputStream(jarEntry), jarEntry.getName());
                        }
                    }
                    jarFile.close();
                } catch (Exception err) {
                    System.out.println("error jar is " + jar.getAbsolutePath());
                    err.printStackTrace();
                }
            }
        }

        for (Directory directory : getAllDirectories().get()) {
            for (File file : directory.getAsFileTree()) {
                String name = directory.getAsFile().toURI().relativize(file.toURI()).getPath().replace(File.separatorChar, '/');
                if (name.contains("META-INF/") || !addedEntries.add(name)) {
                    // 如果已添加该条目，则跳过
                    continue;
                }
                TheRouterInjects.allClass.add(name);
                if (!name.contains("$")) {
                    if (name.contains(TheRouterInjects.PREFIX_ROUTER_MAP)) {
                        TheRouterInjects.routeSet.add(name.replaceAll(".class", ""));
                        try {
                            ClassReader reader = new ClassReader(new FileInputStream(file));
                            ClassNode cn = new ClassNode();
                            reader.accept(cn, 0);
                            Map<String, String> fieldMap = new HashMap<>();
                            int count = 0;
                            List<FieldNode> fieldList = cn.fields;
                            for (FieldNode fieldNode : fieldList) {
                                if (TheRouterInjects.FIELD_ROUTER_MAP_COUNT.equals(fieldNode.name)) {
                                    count = Integer.parseInt(fieldNode.value.toString());
                                }
                                if (fieldNode.name.startsWith(TheRouterInjects.FIELD_ROUTER_MAP)) {
                                    fieldMap.put(fieldNode.name, fieldNode.value.toString());
                                }
                            }
                            if (fieldMap.size() == 1 && count == 0) {  // old version
                                TheRouterInjects.routeMapStringSet.addAll(fieldMap.values());
                            } else if (fieldMap.size() == count) {    // new version
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < count; i++) {
                                    stringBuilder.append(fieldMap.get(TheRouterInjects.FIELD_ROUTER_MAP + i));
                                }
                                TheRouterInjects.routeMapStringSet.add(stringBuilder.toString());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (name.contains(TheRouterInjects.PREFIX_SERVICE_PROVIDER)) {
                        TheRouterInjects.serviceProvideMap.put(name.replaceAll(".class", ""), BuildConfig.VERSION);
                        try {
                            ClassReader reader = new ClassReader(new FileInputStream(file));
                            ClassNode cn = new ClassNode();
                            reader.accept(cn, 0);
                            List<FieldNode> fieldList = cn.fields;
                            for (FieldNode fieldNode : fieldList) {
                                if (TheRouterInjects.FIELD_FLOW_TASK_JSON.equals(fieldNode.name)) {
                                    Map<String, String> map = TheRouterInjects.gson.fromJson(fieldNode.value.toString(), HashMap.class);
                                    TheRouterInjects.flowTaskMap.putAll(map);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (name.contains(TheRouterInjects.SUFFIX_AUTOWIRED)) {
                        TheRouterInjects.autowiredSet.add(name.replaceAll(".class", ""));
                    }
                }
                mergeClassTransform(new FileInputStream(file), name);
            }
        }

        boolean change1 = ClassCacheUtils.write(TheRouterInjects.serviceProvideMap.keySet(), new File(therouterBuildFolder, "serviceProvide.therouter"));
        boolean change2 = ClassCacheUtils.write(TheRouterInjects.autowiredSet, new File(therouterBuildFolder, "autowired.therouter"));
        boolean change3 = ClassCacheUtils.write(TheRouterInjects.routeSet, new File(therouterBuildFolder, "route.therouter"));
        if (change1 || change2 || change3) {
            onCacheChange();
        }

        asmTheRouterJar(theRouterJar, theRouterServiceProvideInjecter);

        Set<RouteItem> pageSet = new HashSet<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (String routeMapString : TheRouterInjects.routeMapStringSet) {
            pageSet.addAll(gson.fromJson(routeMapString, new TypeToken<List<RouteItem>>() {
            }.getType()));
        }

        // 如果文件存在
        if (assetRouteMap != null && assetRouteMap.exists()) {
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
                    assetString = ResourceGroovyMethods.getText(assetRouteMap, StandardCharsets.UTF_8.displayName());
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
            if (assetRouteMap != null) {
                // 如果文件不存在，创建父目录和文件
                System.out.println("---------TheRouter route map does not exist: /assets/therouter/routeMap.json-------");
                try {
                    assetRouteMap.getParentFile().mkdirs();
                    assetRouteMap.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                if (TheRouterPluginUtils.needCheckRouteItemClass(theRouterExtension.checkRouteMap)) {
                    boolean classNotFound = true;

                    // 遍历 mergeClass 以检查 routeItem.className
                    for (String item : TheRouterInjects.allClass) {
                        // routeItem.className 格式为 com.therouter.demo.shell.TestActivity
                        // item 格式为 com/therouter/demo/shell/TestActivity
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
        ResourceGroovyMethods.write(assetRouteMap, json, StandardCharsets.UTF_8.displayName());
        System.out.println("---------TheRouter create new route map--------------");

        Map<String, Set<String>> flowTaskDependMap = new HashMap<>();
        for (String key : TheRouterInjects.flowTaskMap.keySet()) {
            Set<String> value = flowTaskDependMap.get(key);

            if (value == null) {
                value = new HashSet<>();
            }

            String dependsOn = TheRouterInjects.flowTaskMap.get(key);
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
                    if (!flowTaskDependMap.containsKey(dependency)
                            && !dependency.equalsIgnoreCase(TheRouterFlowTask.APP_ONSPLASH)
                            && !dependency.equalsIgnoreCase(TheRouterFlowTask.THEROUTER_INITIALIZATION)
                            && !dependency.equalsIgnoreCase(TheRouterFlowTask.BEFORE_THEROUTER_INITIALIZATION)) {
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

        if (theRouterExtension.showFlowDepend) {
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

    public void onCacheChange() {
        System.out.println(LogUI.C_ERROR.getValue() + "TheRouter：编译缓存发生变化，建议重新编译，避免新增改动未生效" + LogUI.E_NORMAL.getValue());
    }

    public void asmTheRouterJar(File theRouterJar, JarEntry theRouterServiceProvideInjecter)
            throws ClassNotFoundException, IOException {
        // 增量模式时全部不处理，只用来遍历并标记全部class，由下次编译时AsmClassVisitorFactory处理
    }

    public void mergeClassTransform(InputStream inputStream, String name)
            throws ClassNotFoundException, IOException {
        // 增量模式时全部不处理，只用来遍历并标记全部class，由下次编译时AsmClassVisitorFactory处理
    }

    // 一个辅助方法，用来从 URI 中提取查询参数
    public static Map<String, String> extractQueryParams(URI uri) {
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