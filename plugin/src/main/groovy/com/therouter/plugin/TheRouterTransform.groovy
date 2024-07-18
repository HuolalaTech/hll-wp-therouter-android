package com.therouter.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.therouter.plugin.utils.TheRouterPluginUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * Created by ZhangTao on 18/2/24.
 */

public class TheRouterTransform extends Transform {

    private Project mProject
    private final Set<String> allClass = new HashSet<>()
    private final TheRouterExtension theRouterExtension = new TheRouterExtension();

    public TheRouterTransform(Project p) {
        this.mProject = p
    }

    @Override
    String getName() {
        return "TheRouter"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider,
                   boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        println("TheRouter编译插件：${LogUI.C_BLACK_GREEN.value}" + "cn.therouter:${BuildConfig.NAME}:${BuildConfig.VERSION}" + "${LogUI.E_NORMAL.value}")
        println "JDK Version::" + System.getProperty("java.version")
        println "Gradle Version::${mProject.gradle.gradleVersion}"
        println "checkRouteMap::${theRouterExtension.checkRouteMap}"
        println "checkFlowDepend::${theRouterExtension.checkFlowDepend}"

        println("----------------------TheRouter build start------------------------------")

        if (mProject.TheRouter) {
            theRouterExtension.debug = Boolean.valueOf(mProject.TheRouter.debug)
            theRouterExtension.checkRouteMap = mProject.TheRouter.checkRouteMap
            theRouterExtension.checkFlowDepend = mProject.TheRouter.checkFlowDepend
            theRouterExtension.showFlowDepend = mProject.TheRouter.showFlowDepend
        }

        theRouterTransform(inputs, outputProvider)
        println("----------------------TheRouter build finish-----------------------------")
    }

    private void theRouterTransform(Collection<TransformInput> inputs, TransformOutputProvider outputProvider) {
        def theRouterClassOutputFile
        Set<String> routeMapStringSet = new HashSet<>();
        Map<String, String> flowTaskMap = new HashMap<>();
        outputProvider.deleteAll()
        File folder = new File(mProject.buildDir, "therouter")
        folder.mkdirs()
        inputs.each { TransformInput input ->
            // 遍历jar包
            input.jarInputs.each { JarInput jarInput ->
                String jarName = jarInput.file.name.toLowerCase()
                String cacheName = jarName
                String logInfo = "---------TheRouter handle jar " + jarName + "  "
                if (jarInput.scopes.contains(QualifiedContent.Scope.SUB_PROJECTS)) {
                    cacheName = jarInput.name
                }
                File cacheFile = new File(folder, cacheName)
                if (cacheFile.exists()) {
                    if (jarInput.getStatus() == Status.NOTCHANGED) {
                        logInfo = logInfo + LogUI.C_INFO.value + jarInput.getStatus() + LogUI.E_NORMAL.value
                    } else {
                        cacheFile.delete()
                        logInfo = logInfo + LogUI.C_WARN.value + jarInput.getStatus() + LogUI.E_NORMAL.value
                    }
                } else {
                    logInfo = logInfo + LogUI.C_WARN.value + "EMPTY_CACHE" + LogUI.E_NORMAL.value
                }
                debugLog(logInfo)

                File dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                JarInfo jarInfo;
                if (cacheFile.exists()) {
                    jarInfo = TheRouterInjects.fromCache(cacheFile)
                } else {
                    jarInfo = TheRouterInjects.tagJar(jarInput.file)
                    TheRouterInjects.toCache(cacheFile, jarInfo)
                }
                if (jarInfo.isTheRouterJar) {
                    theRouterClassOutputFile = dest
                    debugLog("---------TheRouter jar path is " + dest.absolutePath)
                }
                routeMapStringSet.addAll(jarInfo.routeMapStringFromJar)
                flowTaskMap.putAll(jarInfo.flowTaskMapFromJar)
                allClass.addAll(jarInfo.allJarClass)
                if (jarInput.getStatus() != Status.NOTCHANGED && dest.exists()) {
                    FileUtils.forceDelete(dest)
                }
                if (jarInput.getStatus() != Status.REMOVED) {
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
            // 遍历源码
            input.directoryInputs.each { DirectoryInput directoryInput ->
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                FileUtils.forceMkdir(dest)
                SourceInfo sourceInfo = TheRouterInjects.tagClass(directoryInput.file.absolutePath)
                allClass.addAll(sourceInfo.allSourceClass)
                routeMapStringSet.addAll(sourceInfo.routeMapStringFromSource)
                flowTaskMap.putAll(sourceInfo.flowTaskMapFromSource)
                if (dest.exists()) {
                    FileUtils.forceDelete(dest)
                }
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }

        if (theRouterClassOutputFile) {
            println("---------TheRouter ASM, spend：----------------------")
            TheRouterInjects.injectClassCode(theRouterClassOutputFile)
        }

        Set<RouteItem> pageSet = new HashSet<>()
        Gson gson = new GsonBuilder().setPrettyPrinting().create()
        routeMapStringSet.each {
            pageSet.addAll((List<RouteItem>) gson.fromJson(it, new TypeToken<List<RouteItem>>() {
            }.getType()))
        }
        // 让第三方Activity也支持路由，第三方页面的路由表可以在assets中添加
        File assetRouteMap = new File(mProject.projectDir, "src/main/assets/therouter/routeMap.json")
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
        pageSet.each {
            routeItem ->
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
                    for (String item : allClass) {
                        if (item.contains(routeItem.className)) {
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

        println("---------TheRouter create new route map--------------------")

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
                        } else if (TheRouterPlugin.ERROR.equalsIgnoreCase(theRouterExtension.checkFlowDepend)) {
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

        if (theRouterExtension.showFlowDepend) {
            flowTaskDependMap.keySet().each {
                TheRouterPluginUtils.fillNode(createNode(flowTaskDependMap, it), null)
            }

            println()
            println("${LogUI.C_WARN.value}" + "TheRouter:: FlowTask::dependency   " + "${LogUI.E_NORMAL.value}")
            println("${LogUI.C_WARN.value}" + "==========================================" + "${LogUI.E_NORMAL.value}")
            dependStack.sort().each {
                println("${LogUI.C_WARN.value}" + "[Root --> $it]" + "${LogUI.E_NORMAL.value}")
            }
            println("${LogUI.C_WARN.value}" + "==========================================" + "${LogUI.E_NORMAL.value}")
            println()
        }

        println("---------TheRouter check flow task map--------------")
    }

    private void debugLog(String log) {
        if (theRouterExtension.debug) {
            println(log)
        }
    }
}
