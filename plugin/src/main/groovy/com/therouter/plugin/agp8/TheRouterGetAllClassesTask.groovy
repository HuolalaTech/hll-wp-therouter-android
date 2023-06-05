package com.therouter.plugin.agp8

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.therouter.plugin.AddCodeVisitor
import com.therouter.plugin.BuildConfig
import com.therouter.plugin.LogUI
import com.therouter.plugin.RouteItem
import com.therouter.plugin.TheRouterPlugin
import com.therouter.plugin.utils.TheRouterPluginUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

abstract class TheRouterGetAllClassesTask extends DefaultTask {

    @InputFiles
    abstract ListProperty<RegularFile> getAllJars();

    @InputFiles
    abstract ListProperty<Directory> getAllDirectories();

    @OutputFiles
    abstract RegularFileProperty getOutput();

    @TaskAction
    void taskAction() {
        Set<String> routeMapStringSet = new HashSet<>();
        Map<String, String> flowTaskMap = new HashMap<>();
        long startFirst = System.currentTimeMillis()
        long start = System.currentTimeMillis()
        println("---------TheRouter transform start-------------------------------------------")
        File theRouterJar = null;
        JarEntry theRouterServiceProvideInjecter = null;
        OutputStream jarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(getOutput().get().getAsFile())))
        allJars.get().each { file ->
            JarFile jarFile = new JarFile(file.asFile)
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                JarEntry jarEntry = e.nextElement();
                try {
                    if (jarEntry.name.contains("TheRouterServiceProvideInjecter")) {
                        theRouterJar = file.asFile
                        theRouterServiceProvideInjecter = jarEntry
                    } else {
                        tag(jarEntry.name)
                        jarOutput.putNextEntry(new JarEntry(jarEntry.name))
                        jarFile.getInputStream(jarEntry).withCloseable { inputStream ->
                            if (isRouterMap(jarEntry.name)) {
                                ClassReader reader = new ClassReader(new JarFile(file.asFile).getInputStream(new JarEntry(jarEntry.name)))
                                ClassNode cn = new ClassNode()
                                reader.accept(cn, 0)
                                List<FieldNode> fieldList = cn.fields
                                for (FieldNode fieldNode : fieldList) {
                                    if (TheRouterPlugin.FIELD_ROUTER_MAP == fieldNode.name) {
                                        println("---------TheRouter in jar get route map from: ${jarEntry.name}-------------------------------")
                                        routeMapStringSet.add(fieldNode.value)
                                    }
                                }
                            } else if (isServiceProvider(jarEntry.name)) {
                                ClassReader reader = new ClassReader(new JarFile(file.asFile).getInputStream(new JarEntry(jarEntry.name)))
                                ClassNode cn = new ClassNode()
                                reader.accept(cn, 0)
                                List<FieldNode> fieldList = cn.fields
                                for (FieldNode fieldNode : fieldList) {
                                    if (TheRouterPlugin.FIELD_FLOW_TASK_JSON == fieldNode.name) {
                                        println("---------TheRouter in jar get flow task json from: ${jarEntry.name}-------------------------------")
                                        Map<String, String> map = TheRouterPlugin.gson.fromJson(fieldNode.value, HashMap.class);
                                        flowTaskMap.putAll(map)
                                    }
                                }
                            }
                            jarOutput << inputStream
                        }
                        jarOutput.closeEntry()
                    }
                } catch (Exception xx) {
                }
            }
            jarFile.close()
        }

        getAllDirectories().get().each { directory ->
            directory.asFile.traverse(type: groovy.io.FileType.FILES) { file ->
                String relativePath = directory.asFile.toURI()
                        .relativize(file.toURI())
                        .getPath()
                        .replace(File.separatorChar, '/' as char)
                jarOutput.putNextEntry(new JarEntry(relativePath))
                tag(relativePath)
                new FileInputStream(file).withCloseable { inputStream ->
                    if (isRouterMap(relativePath)) {
                        ClassReader reader = new ClassReader(new FileInputStream(file.absolutePath))
                        ClassNode cn = new ClassNode()
                        reader.accept(cn, 0)
                        List<FieldNode> fieldList = cn.fields
                        for (FieldNode fieldNode : fieldList) {
                            if (TheRouterPlugin.FIELD_ROUTER_MAP == fieldNode.name) {
                                println("---------TheRouter in jar get route map from: ${relativePath}-------------------------------")
                                routeMapStringSet.add(fieldNode.value)
                            }
                        }
                    } else if (isServiceProvider(relativePath)) {
                        ClassReader reader = new ClassReader(new FileInputStream(file.absolutePath))
                        ClassNode cn = new ClassNode()
                        reader.accept(cn, 0)
                        List<FieldNode> fieldList = cn.fields
                        for (FieldNode fieldNode : fieldList) {
                            if (TheRouterPlugin.FIELD_FLOW_TASK_JSON == fieldNode.name) {
                                println("---------TheRouter in jar get flow task json from: ${relativePath}-------------------------------")
                                Map<String, String> map = TheRouterPlugin.gson.fromJson(fieldNode.value, HashMap.class);
                                flowTaskMap.putAll(map)
                            }
                        }
                    }
                    jarOutput << inputStream
                }
                jarOutput.closeEntry()
            }
        }

        if (theRouterJar != null && theRouterServiceProvideInjecter != null) {
            JarFile jarFile = new JarFile(theRouterJar)
            jarOutput.putNextEntry(new JarEntry(theRouterServiceProvideInjecter.name))
            jarFile.getInputStream(theRouterServiceProvideInjecter).withCloseable {
                ClassReader cr = new ClassReader(it)
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
                AddCodeVisitor cv = new AddCodeVisitor(cw, TheRouterPlugin.serviceProvideMap, TheRouterPlugin.autowiredSet, TheRouterPlugin.routeSet, false)
                cr.accept(cv, ClassReader.SKIP_DEBUG)
                byte[] bytes = cw.toByteArray()
                jarOutput.write(bytes)
            }
            jarOutput.closeEntry()
        }
        jarOutput.close()
        long time = System.currentTimeMillis() - start;
        println("---------TheRouter ASM, spend：${time}ms----------------------")
        start = System.currentTimeMillis()


        Set<RouteItem> pageSet = new HashSet<>()
        Gson gson = new GsonBuilder().setPrettyPrinting().create()
        routeMapStringSet.each {
            pageSet.addAll((List<RouteItem>) gson.fromJson(it, new TypeToken<List<RouteItem>>() {
            }.getType()))
        }
        // 让第三方Activity也支持路由，第三方页面的路由表可以在assets中添加
        File assetRouteMap = new File(TheRouterPlugin.mProject.projectDir, "src/main/assets/therouter/routeMap.json")
        if (assetRouteMap.exists()) {
            String assetString = FileUtils.readFileToString(assetRouteMap)
            println("---------TheRouter get route map from: /assets/therouter/routeMap.json-------")
            try {
                List<RouteItem> assetsList = (List<RouteItem>) gson.fromJson(assetString, new TypeToken<List<RouteItem>>() {
                }.getType())
                for (RouteItem item : assetsList) {
                    if (!pageSet.contains(item)) {
                        pageSet.add(item)
                    }
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
        } else {
            println("---------TheRouter route map does not exist: /assets/therouter/routeMap.json-------")
            assetRouteMap.getParentFile().mkdirs()
            assetRouteMap.createNewFile()
        }

        Map<String, List<RouteItem>> result = new HashMap<>()
        // 检查url合法性
        pageSet.each { routeItem ->
            String url = routeItem.path
            if (url.contains("?")) {
                URI uri = new URI(routeItem.path)
                def map = uri.getProperties()
                for (key in map.keySet()) {
                    routeItem.params.put(key, map.get(key))
                }
                url = url.substring(0, url.indexOf('?'))
            }
            List<RouteItem> routeList = result.get(url)
            if (routeList == null) {
                routeList = new ArrayList<>()
                routeList.add(routeItem)
                result.put(url, routeList)
            }
        }
        // 检查路由表合法性
        result.values().each {
            String className = null
            it.each { routeItem ->
                if (className == null) {
                    className = routeItem.className
                } else if (className != routeItem.className) {
                    throw new RuntimeException("Multiple Activity to single Url: $className and ${routeItem.className}")
                }
                if (!TheRouterPluginUtils.getLocalProperty(TheRouterPlugin.mProject, TheRouterPlugin.CHECK_ROUTE_MAP).isEmpty()) {
                    boolean classNotFound = true
                    for (String item : allClass) {
                        if (item.contains(routeItem.className)) {
                            classNotFound = false
                            break
                        }
                    }
                    if (classNotFound) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(TheRouterPlugin.mProject, TheRouterPlugin.CHECK_ROUTE_MAP))) {
                            throw new ClassNotFoundException(routeItem.className + " in /assets/therouter/routeMap.json")
                        } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(TheRouterPlugin.mProject, TheRouterPlugin.CHECK_ROUTE_MAP))) {
                            println("${LogUI.C_WARN.value}[${routeItem.className} in /assets/therouter/routeMap.json]${LogUI.E_NORMAL.value}")
                        }
                    }
                }
            }
        }
        List<RouteItem> pageList = new ArrayList<>(pageSet)
        Collections.sort(pageList)
        String json = gson.toJson(pageList)
        FileUtils.write(assetRouteMap, json, false)
        time = System.currentTimeMillis() - start
        println("---------TheRouter create new route map, spend:${time}ms--------------")
        start = System.currentTimeMillis()

        Map<String, Set<String>> flowTaskDependMap = new HashMap<>();
        flowTaskMap.keySet().each {
            Set<String> value = flowTaskDependMap.get(it)
            if (value == null) {
                value = new HashSet<>()
            }
            String dependsOn = flowTaskMap.get(it)
            if (!dependsOn.isBlank()) {
                dependsOn.split(",").each { depend ->
                    if (!depend.isBlank()) {
                        value.add(depend.trim())
                    }
                }
            }
            flowTaskDependMap.put(it, value)
        }

        if (!TheRouterPluginUtils.getLocalProperty(TheRouterPlugin.mProject, TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND).isEmpty()) {
            flowTaskDependMap.values().each { taskName ->
                flowTaskDependMap[taskName].each {
                    if (!flowTaskDependMap.containsKey(it)) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(TheRouterPlugin.mProject, TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))) {
                            throw new RuntimeException("\n\n==========================================" +
                                    "\nTheRouter:: FlowTask::   " +
                                    "\nCan not found Task: [$it] from $taskName dependsOn" +
                                    "\n==========================================\n\n")
                        } else if (TheRouterPlugin.ERROR.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(TheRouterPlugin.mProject, TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))) {
                            println()
                            println("${LogUI.C_WARN.value}" + "==========================================" + "${LogUI.E_NORMAL.value}")
                            println("${LogUI.C_WARN.value}" + "TheRouter:: FlowTask::   " + "${LogUI.E_NORMAL.value}")
                            println("${LogUI.C_WARN.value}" + "Can not found Task: [$it] from $taskName dependsOn" + "${LogUI.E_NORMAL.value}")
                            println("${LogUI.C_WARN.value}" + "==========================================" + "${LogUI.E_NORMAL.value}")
                            println()
                        }
                    }
                }
            }
        }

        flowTaskDependMap.keySet().each {
            TheRouterPluginUtils.fillTodoList(flowTaskDependMap, it)
        }

        if (Boolean.valueOf(TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.SHOW_FLOW_DEPEND))) {
            flowTaskDependMap.keySet().each {
                TheRouterPluginUtils.fillNode(TheRouterPluginUtils.createNode(flowTaskDependMap, it), null)
            }

            println()
            println("${LogUI.C_WARN.value}" + "TheRouter:: FlowTask::dependency   " + "${LogUI.E_NORMAL.value}")
            println("${LogUI.C_WARN.value}" + "==========================================" + "${LogUI.E_NORMAL.value}")
            TheRouterPluginUtils.dependStack.sort().each {
                println("${LogUI.C_WARN.value}" + "[Root --> $it]" + "${LogUI.E_NORMAL.value}")
            }
            println("${LogUI.C_WARN.value}" + "==========================================" + "${LogUI.E_NORMAL.value}")
            println()

        }

        time = System.currentTimeMillis() - start
        println("---------TheRouter check flow task map, spend:${time}ms--------------")

        time = System.currentTimeMillis() - startFirst;
        println("---------TheRouter transform finish, spend:${time}ms------------------------------------------")
    }

    static void tag(String className) {
        className = className.replaceAll(TheRouterPlugin.DOT_CLASS, "")
        if (isAutowired(className)) {
            TheRouterPlugin.autowiredSet.add(className)
        } else if (isRouterMap(className)) {
            TheRouterPlugin.routeSet.add(className)
        } else if (isServiceProvider(className)) {
            TheRouterPlugin.serviceProvideMap.put(className, BuildConfig.VERSION)
        }
    }

    static boolean isAutowired(String className) {
        return className.endsWith(TheRouterPlugin.SUFFIX_AUTOWIRED)
    }

    static boolean isRouterMap(String className) {
        return (className.startsWith(TheRouterPlugin.PREFIX_ROUTER_MAP)
                || className.startsWith("a/" + TheRouterPlugin.PREFIX_ROUTER_MAP))
                && !className.contains("\$")
    }

    static boolean isServiceProvider(String className) {
        return (className.startsWith(TheRouterPlugin.PREFIX_SERVICE_PROVIDER)
                || className.startsWith("a/" + TheRouterPlugin.PREFIX_SERVICE_PROVIDER))
                && !className.contains("\$")
    }
}