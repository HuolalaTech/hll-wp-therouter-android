package com.therouter.plugin

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by ZhangTao on 18/2/24.
 */

class TheRouterInjects {

    private static Map<String, String> serviceProvideMap = new HashMap<>()
    private static Set<String> autowiredSet = new HashSet<>()
    private static Set<String> routeSet = new HashSet<>()

    private static final Gson gson = new Gson()

    private static final PREFIX_SERVICE_PROVIDER = "ServiceProvider__TheRouter__"
    private static final PREFIX_ROUTER_MAP = "RouterMap__TheRouter__"
    private static final SUFFIX_AUTOWIRED_DOT_CLASS = "__TheRouter__Autowired.class"
    private static final FIELD_FLOW_TASK_JSON = "FLOW_TASK_JSON"
    private static final FIELD_APT_VERSION = "THEROUTER_APT_VERSION"
    private static final FIELD_ROUTER_MAP = "ROUTERMAP"
    private static final UNKNOWN_VERSION = "unspecified"
    private static final NOT_FOUND_VERSION = "0.0.0"
    private static final DOT_CLASS = ".class"

    /**
     * 标记当前jar中是否有要处理的类，生成类总共三种：RouterMap、ServiceProvider、Autowired
     * @param jarFile
     * @return
     */
    public static JarInfo tagJar(File jarFile) {
        JarInfo jarInfo = new JarInfo()
        if (jarFile) {
            def file = new JarFile(jarFile)
            Enumeration enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                jarInfo.allJarClass.add(jarEntry.name.replaceAll("/", "."))
                if (jarEntry.name.contains(PREFIX_SERVICE_PROVIDER)) {
                    int start = jarEntry.name.indexOf(PREFIX_SERVICE_PROVIDER)
                    int end = jarEntry.name.length() - DOT_CLASS.length()
                    String className = jarEntry.name.substring(start, end)
                            .replace('\\', '.')
                            .replace('/', '.')
                    if (className.indexOf('$') <= 0) { // 只处理非内部类
                        InputStream inputStream = file.getInputStream(jarEntry)
                        ClassReader reader = new ClassReader(inputStream)
                        ClassNode cn = new ClassNode()
                        reader.accept(cn, 0)
                        List<FieldNode> fieldList = cn.fields
                        String aptVersion = NOT_FOUND_VERSION
                        for (FieldNode fieldNode : fieldList) {
                            if (FIELD_FLOW_TASK_JSON == fieldNode.name) {
                                println("---------TheRouter in jar get flow task json from: ${jarEntry.name}-------------------------------")
                                Map<String, String> map = gson.fromJson(fieldNode.value, HashMap.class);
                                jarInfo.flowTaskMapFromJar.putAll(map)
                            } else if (FIELD_APT_VERSION == fieldNode.name) {
                                aptVersion = fieldNode.value
                            }
                        }
                        if (!serviceProvideMap.containsKey(className) || aptVersion != NOT_FOUND_VERSION) {
                            serviceProvideMap.put(className, aptVersion)
                        }
                    }
                } else if (jarEntry.name.contains(SUFFIX_AUTOWIRED_DOT_CLASS)) {
                    String className = jarEntry.name
                            .replace(DOT_CLASS, "")
                            .replace('\\', '.')
                            .replace('/', '.')
                    autowiredSet.add(className)
                } else if (jarEntry.name.contains("TheRouterServiceProvideInjecter")) {
                    jarInfo.isTheRouterJar = true
                } else if (jarEntry.name.contains(PREFIX_ROUTER_MAP)) {
                    routeSet.add(jarEntry.name)
                    InputStream inputStream = file.getInputStream(jarEntry)
                    ClassReader reader = new ClassReader(inputStream)
                    ClassNode cn = new ClassNode()
                    reader.accept(cn, 0)
                    List<FieldNode> fieldList = cn.fields
                    for (FieldNode fieldNode : fieldList) {
                        if (FIELD_ROUTER_MAP == fieldNode.name) {
                            println("---------TheRouter in jar get route map from: ${jarEntry.name}-------------------------------")
                            jarInfo.routeMapStringFromJar.add(fieldNode.value)
                        }
                    }
                }
            }
        }
        return jarInfo
    }

    public static SourceInfo tagClass(String path) {
        SourceInfo sourceInfo = new SourceInfo();
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse {
                sourceInfo.allSourceClass.add(it.absolutePath.replace(File.separator, "."))
                if (it.absolutePath.contains(PREFIX_SERVICE_PROVIDER)) {
                    int start = it.absolutePath.indexOf(PREFIX_SERVICE_PROVIDER)
                    int end = it.absolutePath.length() - DOT_CLASS.length()
                    String className = it.absolutePath.substring(start, end)
                            .replace(File.separator, ".")
                    if (className.indexOf('$') > 0) {
                        className = className.substring(0, className.indexOf('$'))
                    }
                    FileInputStream inputStream = new FileInputStream(it)
                    ClassReader reader = new ClassReader(inputStream)
                    ClassNode cn = new ClassNode()
                    reader.accept(cn, 0)
                    List<FieldNode> fieldList = cn.fields
                    String aptVersion = NOT_FOUND_VERSION
                    for (FieldNode fieldNode : fieldList) {
                        if (FIELD_FLOW_TASK_JSON == fieldNode.name) {
                            println("---------TheRouter in source get flow task json from: ${it.name}-------------------------------")
                            Map<String, String> map = gson.fromJson(fieldNode.value, HashMap.class);
                            sourceInfo.flowTaskMapFromSource.putAll(map)
                        } else if (FIELD_APT_VERSION == fieldNode.name) {
                            aptVersion = fieldNode.value
                        }
                    }
                    if (!serviceProvideMap.containsKey(className) || aptVersion != NOT_FOUND_VERSION) {
                        serviceProvideMap.put(className, aptVersion)
                    }
                } else if (it.absolutePath.contains(SUFFIX_AUTOWIRED_DOT_CLASS)) {
                    String className = it.absolutePath
                            .replace(path, "")
                            .replace(DOT_CLASS, "")
                            .replace(File.separator, ".")
                            .replace("classes.", "")
                    if (className.startsWith(".")) {
                        className = className.substring(1)
                    }
                    autowiredSet.add(className)
                } else if (it.absolutePath.contains(PREFIX_ROUTER_MAP)) {
                    int start = it.absolutePath.indexOf(PREFIX_ROUTER_MAP)
                    int end = it.absolutePath.length() - DOT_CLASS.length()
                    String className = it.absolutePath.substring(start, end)
                            .replace(File.separator, ".")
                    // 因为absolutePath过滤的时候是直接以类名过滤，就把包名去掉了
                    // 包名一定是a，所以这里补回来
                    routeSet.add("a/" + className)

                    FileInputStream inputStream = new FileInputStream(it)
                    ClassReader reader = new ClassReader(inputStream)
                    ClassNode cn = new ClassNode();
                    reader.accept(cn, 0);
                    List<FieldNode> fieldList = cn.fields;
                    for (FieldNode fieldNode : fieldList) {
                        if (FIELD_ROUTER_MAP == fieldNode.name) {
                            println("---------TheRouter in source get route map from: ${it.name}-------------------------------")
                            sourceInfo.routeMapStringFromSource.add(fieldNode.value)
                        }
                    }
                }
            }
        }
        return sourceInfo
    }

    /**
     * 开始修改 TheRouterServiceProvideInjecter 类
     */
    static void injectClassCode(File inputJarFile, boolean isIncremental) {
        long start = System.currentTimeMillis()
        def optJarFile = new File(inputJarFile.getParent(), inputJarFile.name + ".opt")
        def inputJar = new JarFile(inputJarFile)
        Enumeration enumeration = inputJar.entries()
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJarFile))
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement();
            String entryName = jarEntry.getName()
            ZipEntry zipEntry = new ZipEntry(entryName)
            jarOutputStream.putNextEntry(zipEntry)
            InputStream inputStream = inputJar.getInputStream(jarEntry)
            byte[] bytes
            if (entryName.contains("TheRouterServiceProvideInjecter")) {
                ClassReader cr = new ClassReader(inputStream)
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
                AddCodeVisitor cv = new AddCodeVisitor(cw, serviceProvideMap, autowiredSet, routeSet, isIncremental)
                cr.accept(cv, ClassReader.SKIP_DEBUG)
                bytes = cw.toByteArray()
            } else {
                bytes = IOUtils.toByteArray(inputStream)
            }
            jarOutputStream.write(bytes)
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        inputJar.close()
        inputJarFile.delete()
        optJarFile.renameTo(inputJarFile)
        optJarFile.delete()
        long time = System.currentTimeMillis() - start
        println("---------TheRouter inject TheRouterServiceProvideInjecter.class, spend：${time}ms----------------------")
    }
}
