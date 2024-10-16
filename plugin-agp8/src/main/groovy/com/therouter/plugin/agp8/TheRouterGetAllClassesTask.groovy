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
import org.gradle.api.tasks.OutputFile
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

    private static final Set<String> allClass = new HashSet<>()
    private static final Set<String> mergeClass = new HashSet<>()
    private static final TheRouterExtension theRouterExtension = new TheRouterExtension();

    @InputFiles
    abstract ListProperty<RegularFile> getAllJars();

    @InputFiles
    abstract ListProperty<Directory> getAllDirectories();

    @OutputFile
    abstract RegularFileProperty getOutput();

    @TaskAction
    void taskAction() {
        if (project.TheRouter) {
            theRouterExtension.debug = Boolean.valueOf(project.TheRouter.debug)
            theRouterExtension.checkRouteMap = project.TheRouter.checkRouteMap
            theRouterExtension.checkFlowDepend = project.TheRouter.checkFlowDepend
            theRouterExtension.showFlowDepend = project.TheRouter.showFlowDepend
        }
        println("TheRouter编译插件：${LogUI.C_BLACK_GREEN.value}" + "cn.therouter:${BuildConfig.NAME}:${BuildConfig.VERSION}" + "${LogUI.E_NORMAL.value}")
        println "JDK Version::" + System.getProperty("java.version")
        println "Gradle Version::${project.gradle.gradleVersion}"
        println "checkRouteMap::${theRouterExtension.checkRouteMap}"
        println "checkFlowDepend::${theRouterExtension.checkFlowDepend}"

        println("----------------------TheRouter build start------------------------------")
        theRouterTransform()
        println("----------------------TheRouter build finish-----------------------------")
    }

    void theRouterTransform() {
        String theRouterInjectEntryName = null;
        Set<String> routeMapStringSet = new HashSet<>();
        Map<String, String> flowTaskMap = new HashMap<>();

        File dest = getOutput().get().asFile
        dest.delete()

        Set<File> changedJarHighLevel = new HashSet<>();
        Set<File> changedJarMiddleLevel = new HashSet<>();
        Set<File> changedJarLowLevel = new HashSet<>();
        File theRouterCacheFolder = new File(project.buildDir, "therouter")
        theRouterCacheFolder.mkdirs()
        File theRouterDest = new File(theRouterCacheFolder, "therouter-dest-cache.jar")
        if (!theRouterDest.exists() || !theRouterExtension.debug) {
            theRouterCacheFolder.deleteDir()
            theRouterCacheFolder.mkdirs()
            theRouterDest.delete()
            mergeClass.clear()
        } else {
            changedJarLowLevel.add(theRouterDest)
        }

        allJars.get().each { file ->
            File jar = file.asFile
            String jarName = jar.name.toLowerCase()
            String cacheName = jarName
            if ("classes.jar".equals(jarName)) {
                cacheName = "classes-" + Math.abs(jar.absolutePath.hashCode()) + ".jar"
            }
            File cacheFile = new File(theRouterCacheFolder, cacheName)
            JarInfo jarInfo;
            String logInfo = "---------TheRouter handle jar " + jarName + "  "
            if (cacheFile.exists()) {
                jarInfo = TheRouterInjects.fromCache(cacheFile)
                if (jar.length() != jarInfo.lastModified) {
                    debugLog(logInfo + LogUI.C_INFO.value + "CHANGED" + LogUI.E_NORMAL.value)
                    cacheFile.delete()
                    jarInfo = TheRouterInjects.tagJar(jar)
                    jarInfo.lastModified = jar.length()
                    TheRouterInjects.toCache(cacheFile, jarInfo)
                    changedJarHighLevel.add(jar)
                } else {
                    debugLog(logInfo + "UP-TO-DATE")
                }
            } else {
                debugLog(logInfo + LogUI.C_WARN.value + "EMPTY_CACHE" + LogUI.E_NORMAL.value)
                jarInfo = TheRouterInjects.tagJar(jar)
                jarInfo.lastModified = jar.length()
                TheRouterInjects.toCache(cacheFile, jarInfo)
                changedJarHighLevel.add(jar)
            }
            if (jarInfo.isTheRouterJar) {
                changedJarMiddleLevel.add(jar)
                theRouterInjectEntryName = jarInfo.theRouterInjectEntryName
                debugLog("---------TheRouter jar path is " + jar.absolutePath)
            }
        }

        OutputStream jarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(dest)))
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
                // a/ServiceProvider__TheRouter__737372.class
                // a/ServiceProvider__TheRouter__737372$Companion$addFlowTask$8.class
                tag(relativePath)
                mergeClass.add(relativePath)
                new FileInputStream(file).withCloseable { inputStream ->
                    if (isRouterMap(relativePath)) { // RouterMap__TheRouter__
                        ClassReader reader = new ClassReader(new FileInputStream(file.absolutePath))
                        ClassNode cn = new ClassNode()
                        reader.accept(cn, 0)
                        List<FieldNode> fieldList = cn.fields
                        for (FieldNode fieldNode : fieldList) {
                            if (TheRouterInjects.FIELD_ROUTER_MAP == fieldNode.name) {
                                println("---------TheRouter in jar get route map from: ${relativePath}-------------------------------")
                                routeMapStringSet.add(fieldNode.value)
                            }
                        }
                    } else if (isServiceProvider(relativePath)) {  // ServiceProvider__TheRouter__
                        ClassReader reader = new ClassReader(new FileInputStream(file.absolutePath))
                        ClassNode cn = new ClassNode()
                        reader.accept(cn, 0)
                        List<FieldNode> fieldList = cn.fields
                        for (FieldNode fieldNode : fieldList) {
                            if (TheRouterInjects.FIELD_FLOW_TASK_JSON == fieldNode.name) {
                                println("---------TheRouter in jar get flow task json from: ${relativePath}-------------------------------")
                                Map<String, String> map = TheRouterInjects.gson.fromJson(fieldNode.value, HashMap.class);
                                flowTaskMap.putAll(map)
                            }
                        }
                    }
                    jarOutput << inputStream
                }
                jarOutput.closeEntry()
            }
        }
        mergeJar(jarOutput, changedJarHighLevel, theRouterInjectEntryName)
        mergeJar(jarOutput, changedJarMiddleLevel, theRouterInjectEntryName)
        mergeJar(jarOutput, changedJarLowLevel, theRouterInjectEntryName)
        jarOutput.close()
        FileUtils.copyFile(dest, theRouterDest)
        mergeClass.clear()
        println("---------TheRouter ASM finish----------------------")

        Set<RouteItem> pageSet = new HashSet<>()
        Gson gson = new GsonBuilder().setPrettyPrinting().create()
        routeMapStringSet.each {
            pageSet.addAll((List<RouteItem>) gson.fromJson(it, new TypeToken<List<RouteItem>>() {
            }.getType()))
        }
        // 让第三方Activity也支持路由，第三方页面的路由表可以在assets中添加
        File assetRouteMap = new File(project.projectDir, "src/main/assets/therouter/routeMap.json")
        if (assetRouteMap.exists()) {
            if (TheRouterPlugin.DELETE.equalsIgnoreCase(theRouterExtension.checkRouteMap)) {
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
                if (!theRouterExtension.checkRouteMap.isEmpty()) {
                    boolean classNotFound = true
                    for (String item : mergeClass) {
                        // routeItem.className 格式为 com.therouter.demo.shell.TestActivity
                        // item 格式为  com/therouter/demo/shell/TestActivity.class
                        if (item.contains(routeItem.className.replaceAll(".", "/"))) {
                            classNotFound = false
                            break
                        }
                    }
                    if (classNotFound) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(theRouterExtension.checkRouteMap)) {
                            throw new ClassNotFoundException(routeItem.className + " in /assets/therouter/routeMap.json")
                        } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(theRouterExtension.checkRouteMap)) {
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

        if (!theRouterExtension.checkFlowDepend.isEmpty()) {
            flowTaskDependMap.values().each { taskName ->
                flowTaskDependMap[taskName].each {
                    if (!flowTaskDependMap.containsKey(it)) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(theRouterExtension.checkFlowDepend)) {
                            throw new RuntimeException("\n\n==========================================" +
                                    "\nTheRouter:: FlowTask::   " +
                                    "\nCan not found Task: [$it] from $taskName dependsOn" +
                                    "\n==========================================\n\n")
                        } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(theRouterExtension.checkFlowDepend)) {
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

        if (Boolean.valueOf(theRouterExtension.showFlowDepend)) {
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

    static void debugLog(String log) {
        if (theRouterExtension.debug) {
            println(log)
        }
    }

    static void mergeJar(JarOutputStream jos, Set<File> jarFiles, String theRouterInjectEntryName) throws IOException {
        for (File jar : jarFiles) {
            JarFile jarFile = new JarFile(jar)
            Enumeration<JarEntry> e = jarFile.entries()
            while (e.hasMoreElements()) {
                try {
                    JarEntry entry = e.nextElement();
                    if (!mergeClass.contains(entry.name)) {
                        jos.putNextEntry(new JarEntry(entry.getName()));
                        jarFile.getInputStream(entry).withCloseable { inputStream ->
                            if (entry.getName().equals(theRouterInjectEntryName)) {
                                ClassReader cr = new ClassReader(inputStream)
                                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
                                AddCodeVisitor cv = new AddCodeVisitor(cw, TheRouterInjects.serviceProvideMap, TheRouterInjects.autowiredSet, TheRouterInjects.routeSet, false)
                                cr.accept(cv, ClassReader.SKIP_DEBUG)
                                byte[] bytes = cw.toByteArray()
                                jos.write(bytes)
                            } else {
                                jos << inputStream
                            }
                        }
                        mergeClass.add(entry.name)
                    }
                } catch (Exception exc) {
                }
                jos.closeEntry();
            }
        }
    }

    void tag(String className) {
        // a/ServiceProvider__TheRouter__737372.class
        className = className.replaceAll(TheRouterInjects.DOT_CLASS, "")
        if (isAutowired(className)) {
            TheRouterInjects.autowiredSet.add(className)
        } else if (isRouterMap(className)) {
            TheRouterInjects.routeSet.add(className)
        } else if (isServiceProvider(className)) {
            TheRouterInjects.serviceProvideMap.put(className, BuildConfig.VERSION)
        }
    }

    static boolean isAutowired(String className) {
        return className.endsWith(TheRouterInjects.SUFFIX_AUTOWIRED)
    }

    static boolean isRouterMap(String className) {
        return (className.startsWith(TheRouterInjects.PREFIX_ROUTER_MAP)
                || className.startsWith("a/" + TheRouterInjects.PREFIX_ROUTER_MAP))
                && !className.contains("\$")
    }

    static boolean isServiceProvider(String className) {
        return (className.startsWith(TheRouterInjects.PREFIX_SERVICE_PROVIDER)
                || className.startsWith("a/" + TheRouterInjects.PREFIX_SERVICE_PROVIDER))
                && !className.contains("\$")
    }
}
