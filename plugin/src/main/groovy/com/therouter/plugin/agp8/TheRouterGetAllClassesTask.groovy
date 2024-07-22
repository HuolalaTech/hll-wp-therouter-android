package com.therouter.plugin.agp8

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.therouter.plugin.*
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

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

abstract class TheRouterGetAllClassesTask extends DefaultTask {

    private static final Set<String> allClass = new HashSet<>()
    private TheRouterExtension theRouterExtension = new TheRouterExtension();

    @InputFiles
    abstract ListProperty<RegularFile> getAllJars();

    @InputFiles
    abstract ListProperty<Directory> getAllDirectories();

    @OutputFiles
    abstract RegularFileProperty getOutput();

    @TaskAction
    void taskAction() {
        println("----------------------TheRouter build start------------------------------")
        if (project.TheRouter) {
            theRouterExtension.debug = Boolean.valueOf(project.TheRouter.debug)
            theRouterExtension.checkRouteMap = project.TheRouter.checkRouteMap
            theRouterExtension.checkFlowDepend = project.TheRouter.checkFlowDepend
            theRouterExtension.showFlowDepend = project.TheRouter.showFlowDepend
        }

        theRouterTransform()
        println("----------------------TheRouter build finish-----------------------------")
    }

    void theRouterTransform() {
        File theRouterClassOutputFile = null;
        JarEntry theRouterServiceProvideInjecter = null;
        Set<String> routeMapStringSet = new HashSet<>();
        Map<String, String> flowTaskMap = new HashMap<>();
        File folder = new File(project.buildDir, "therouter")
        folder.mkdirs()

        File dest = getOutput().get().getAsFile()
        OutputStream jarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(dest)))
        allJars.get().each { file ->
            File jar = file.asFile
            String jarName = jar.name.toLowerCase()
            String cacheName = jarName
            if ("classes.jar".equals(jarName)) {
                cacheName = "" + jar.absolutePath.hashCode()
            }
            File cacheFile = new File(folder, cacheName)
            JarInfo jarInfo;
            String logInfo = "---------TheRouter handle jar " + jarName + "  "
            if (cacheFile.exists()) {
                jarInfo = TheRouterInjects.fromCache(cacheFile)
                if (jar.lastModified() != jarInfo.lastModified) {
                    debugLog(logInfo + LogUI.C_INFO.value + "CHANGED" + LogUI.E_NORMAL.value)
                    cacheFile.delete()
                    jarInfo = TheRouterInjects.tagJar(jar)
                    jarInfo.lastModified = jar.lastModified()
                    TheRouterInjects.toCache(cacheFile, jarInfo)
                } else {
                    debugLog(logInfo + LogUI.C_INFO.value + "NOTCHANGED" + LogUI.E_NORMAL.value)
                }
            } else {
                debugLog(logInfo + LogUI.C_WARN.value + "EMPTY_CACHE" + LogUI.E_NORMAL.value)
                jarInfo = TheRouterInjects.tagJar(jar)
                jarInfo.lastModified = jar.lastModified()
                TheRouterInjects.toCache(cacheFile, jarInfo)
            }
            if (jarInfo.isTheRouterJar) {
                theRouterClassOutputFile = dest
                debugLog("---------TheRouter jar path is " + dest.absolutePath)
            }
            FileUtils.copyFile(jar, dest)
        }

        getAllDirectories().get().each { directory ->
            directory.asFile.traverse(type: groovy.io.FileType.FILES) { file ->
                String relativePath = directory.asFile.toURI()
                        .relativize(file.toURI())
                        .getPath()
                        .replace(File.separatorChar, '/' as char)
                try {
                    jarOutput.putNextEntry(new JarEntry(relativePath))
                } catch (Exception e) {
                }
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

        if (theRouterClassOutputFile != null && theRouterServiceProvideInjecter != null) {
            JarFile jarFile = new JarFile(theRouterClassOutputFile)
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
        println("---------TheRouter ASM----------------------")


        Set<RouteItem> pageSet = new HashSet<>()
        Gson gson = new GsonBuilder().setPrettyPrinting().create()
        routeMapStringSet.each {
            pageSet.addAll((List<RouteItem>) gson.fromJson(it, new TypeToken<List<RouteItem>>() {
            }.getType()))
        }
        // 让第三方Activity也支持路由，第三方页面的路由表可以在assets中添加
        File assetRouteMap = new File(project.projectDir, "src/main/assets/therouter/routeMap.json")
        if (assetRouteMap.exists()) {
            if (TheRouterPlugin.DELETE.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_ROUTE_MAP))) {
                println("---------TheRouter delete route map------------------------------------------")
                assetRouteMap.delete()
                assetRouteMap.createNewFile()
            } else {
                String assetString = FileUtils.readFileToString(assetRouteMap, "UTF-8")
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
                if (!TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_ROUTE_MAP).isEmpty()) {
                    boolean classNotFound = true
                    for (String item : allClass) {
                        if (item.contains(routeItem.className)) {
                            classNotFound = false
                            break
                        }
                    }
                    if (classNotFound) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_ROUTE_MAP))) {
                            throw new ClassNotFoundException(routeItem.className + " in /assets/therouter/routeMap.json")
                        } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_ROUTE_MAP))) {
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
        println("---------TheRouter create new route map--------------")

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

        if (!TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND).isEmpty()) {
            flowTaskDependMap.values().each { taskName ->
                flowTaskDependMap[taskName].each {
                    if (!flowTaskDependMap.containsKey(it)) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))) {
                            throw new RuntimeException("\n\n==========================================" +
                                    "\nTheRouter:: FlowTask::   " +
                                    "\nCan not found Task: [$it] from $taskName dependsOn" +
                                    "\n==========================================\n\n")
                        } else if (TheRouterPlugin.ERROR.equalsIgnoreCase(TheRouterPluginUtils.getLocalProperty(project, TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))) {
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

        println("---------TheRouter check flow task map--------------")
    }

    void debugLog(String log) {
        if (theRouterExtension.debug) {
            println(log)
        }
    }

    void tag(String className) {
        className = className.replaceAll(TheRouterInjects.DOT_CLASS, "")
        if (isAutowired(className)) {
            TheRouterPlugin.autowiredSet.add(className)
        } else if (isRouterMap(className)) {
            TheRouterPlugin.routeSet.add(className)
        } else if (isServiceProvider(className)) {
            TheRouterPlugin.serviceProvideMap.put(className, BuildConfig.VERSION)
        }
        allClass.add(className.replaceAll("/", "."))
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
