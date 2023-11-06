package com.therouter.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * Created by ZhangTao on 18/2/24.
 */

public class TheRouterTransform extends Transform {

    private final Map<String, String> buildProperties = new HashMap<>()

    private Project mProject
    private final Set<String> allClass = new HashSet<>()
    private final Set<String> deletedClass = new HashSet<>()

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
        return Boolean.valueOf(getLocalProperty(TheRouterPlugin.INCREMENTAL))
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider,
                   boolean isIncremental)
            throws IOException, javax.xml.crypto.dsig.TransformException, InterruptedException {
        theRouterTransform(isIncremental, inputs, outputProvider)
    }

    private void theRouterTransform(boolean isIncremental, Collection<TransformInput> inputs, outputProvider) {
        println("TheRouter编译插件：${LogUI.C_BLACK_GREEN.value}" + "cn.therouter:${BuildConfig.NAME}:${BuildConfig.VERSION}" + "${LogUI.E_NORMAL.value}")
        println "当前编译 JDK Version 为::" + System.getProperty("java.version")
        println "本次是增量编译::" + isIncremental
        println "CHECK_ROUTE_MAP::" + getLocalProperty(TheRouterPlugin.CHECK_ROUTE_MAP)
        println "CHECK_FLOW_UNKNOW_DEPEND::" + getLocalProperty(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND)
        long startFirst = System.currentTimeMillis()
        long start = System.currentTimeMillis()
        def theRouterClassOutputFile
        Set<String> routeMapStringSet = new HashSet<>();
        Map<String, String> flowTaskMap = new HashMap<>();
        println("---------TheRouter transform start-------------------------------------------")
        if (!isIncremental) {
            outputProvider.deleteAll()
        }
        inputs.each { TransformInput input ->
            // 遍历jar包
            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name.toLowerCase()
                def dest = outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (!isIncremental) {
                    if (!jarName.contains("com.google.") &&
                            !jarName.contains("org.jetbrains") &&
                            !jarName.contains("androidx.") &&
                            !jarName.contains("io.reactivex") &&
                            !jarName.contains("com.squareup") &&
                            !jarName.contains("glide") &&
                            !jarName.contains("upppay") &&
                            !jarName.contains("amap") &&
                            !jarName.contains("hllim") &&
                            !jarName.contains("pinyin4j") &&
                            !jarName.contains("sensors")) {
                        JarInfo jarInfo = TheRouterInjects.tagJar(jarInput.file)
                        routeMapStringSet.addAll(jarInfo.routeMapStringFromJar)
                        flowTaskMap.putAll(jarInfo.flowTaskMapFromJar)
                        allClass.addAll(jarInfo.allJarClass)
                        if (jarInfo.isTheRouterJar) {
                            theRouterClassOutputFile = dest
                        }
                    }
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest)
                    }
                    FileUtils.copyFile(jarInput.file, dest)
                } else if (isIncremental && (jarInput.getStatus() == Status.CHANGED || jarInput.getStatus() == Status.ADDED)) {
                    JarInfo jarInfo = TheRouterInjects.tagJar(jarInput.file)
                    routeMapStringSet.addAll(jarInfo.routeMapStringFromJar)
                    flowTaskMap.putAll(jarInfo.flowTaskMapFromJar)
                    FileUtils.copyFile(jarInput.file, dest)
                } else if (isIncremental && jarInput.getStatus() == Status.REMOVED) {
                    JarInfo jarInfo = TheRouterInjects.tagJar(jarInput.file)
                    deletedClass.addAll(jarInfo.allJarClass)
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest)
                    }
                }
            }
            // 遍历源码
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dir = directoryInput.file
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                FileUtils.forceMkdir(dest)
                String srcDirPath = dir.absolutePath
                String destDirPath = dest.absolutePath
                if (isIncremental) {
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles()
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue()
                        File inputFile = changedFile.getKey()
                        String destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
                        File destFile = new File(destFilePath)
                        switch (status) {
                            case Status.REMOVED:
                                deletedClass.add(inputFile.absolutePath.replaceAll("/", "."))
                                if (destFile.exists()) {
                                    destFile.delete()
                                }
                                break
                            case Status.ADDED:
                            case Status.CHANGED:
                                SourceInfo sourceInfo = TheRouterInjects.tagClass(inputFile.absolutePath)
                                routeMapStringSet.addAll(sourceInfo.routeMapStringFromSource)
                                flowTaskMap.putAll(sourceInfo.flowTaskMapFromSource)
                                FileUtils.copyFile(inputFile, destFile)
                                break
                            default:
                                break
                        }
                    }
                } else {
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
        }

        if (theRouterClassOutputFile) {
            TheRouterInjects.injectClassCode(theRouterClassOutputFile, isIncremental)
        }

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
        File assetRouteMap = new File(mProject.projectDir, "src/main/assets/therouter/routeMap.json")
        if (assetRouteMap.exists()) {
            if (TheRouterPlugin.DELETE.equalsIgnoreCase(getLocalProperty(TheRouterPlugin.CHECK_ROUTE_MAP))) {
                println("---------TheRouter delete route map------------------------------------------")
                assetRouteMap.delete()
                assetRouteMap.createNewFile()
            } else {
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
                if (!getLocalProperty(TheRouterPlugin.CHECK_ROUTE_MAP).isEmpty()) {
                    // 只在全量编译时检验
                    if (!isIncremental) {
                        boolean classNotFound = true
                        for (String item : allClass) {
                            if (item.contains(routeItem.className)) {
                                classNotFound = false
                                break
                            }
                        }
                        if (classNotFound) {
                            if (TheRouterPlugin.ERROR.equalsIgnoreCase(getLocalProperty(TheRouterPlugin.CHECK_ROUTE_MAP))) {
                                throw new ClassNotFoundException(routeItem.className + " in /assets/therouter/routeMap.json")
                            } else if (TheRouterPlugin.WARNING.equalsIgnoreCase(getLocalProperty(TheRouterPlugin.CHECK_ROUTE_MAP))) {
                                println("${LogUI.C_WARN.value}[${routeItem.className} in /assets/therouter/routeMap.json]${LogUI.E_NORMAL.value}")
                            }
                        }
                    }
                }
            }
        }

        List<RouteItem> pageList
        if (isIncremental && !deletedClass.isEmpty()) {
            pageList = new ArrayList<>(pageSet.size())
            for (RouteItem item : pageSet) {
                boolean needDelete = false
                for (String deletedClassItem : deletedClass) {
                    if (deletedClassItem.contains(item.className)) {
                        needDelete = true
                        break
                    }
                }
                if (!needDelete) {
                    pageList.add(item)
                }
            }
        } else {
            pageList = new ArrayList<>(pageSet)
        }
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

        if (!getLocalProperty(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND).isEmpty()) {
            flowTaskDependMap.values().each { taskName ->
                flowTaskDependMap[taskName].each {
                    if (!flowTaskDependMap.containsKey(it)) {
                        if (TheRouterPlugin.ERROR.equalsIgnoreCase(getLocalProperty(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))) {
                            throw new RuntimeException("\n\n==========================================" +
                                    "\nTheRouter:: FlowTask::   " +
                                    "\nCan not found Task: [$it] from $taskName dependsOn" +
                                    "\n==========================================\n\n")
                        } else if (TheRouterPlugin.ERROR.equalsIgnoreCase(getLocalProperty(TheRouterPlugin.CHECK_FLOW_UNKNOW_DEPEND))) {
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
            fillTodoList(flowTaskDependMap, it)
        }

        if (Boolean.valueOf(getLocalProperty(TheRouterPlugin.SHOW_FLOW_DEPEND))) {
            flowTaskDependMap.keySet().each {
                fillNode(createNode(flowTaskDependMap, it), null)
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

        time = System.currentTimeMillis() - start
        println("---------TheRouter check flow task map, spend:${time}ms--------------")

        time = System.currentTimeMillis() - startFirst;
        println("---------TheRouter transform finish, spend:${time}ms------------------------------------------")
    }

    private final List<String> loopDependStack = new ArrayList<>()

    private void fillTodoList(Map<String, Set<String>> map, String root) {
        Set<String> dependsSet = map[root]
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

    Set<String> dependStack = new HashSet<>()

    private void fillNode(Node node, String root) {
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

    private Node createNode(Map<String, Set<String>> map, String root) {
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

    private String getLog(List<String> list, String root) {
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

    def getLocalProperty(String key) {
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
}
