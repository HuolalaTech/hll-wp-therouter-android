package com.therouter.plugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Created by ZhangTao on 18/2/24.
 */
public class TheRouterInjects {

    // ASM要插入的类，不能包含.class
    public static Map<String, String> serviceProvideMap = new HashMap<>();
    public static Set<String> autowiredSet = new HashSet<>();
    public static Set<String> routeSet = new HashSet<>();

    // 用于编译期代码合法性检查的缓存
    public static final Set<String> routeMapStringSet = new HashSet<>();
    public static final Map<String, String> flowTaskMap = new HashMap<>();
    public static final Set<String> allClass = new HashSet<>();

    public static final Gson gson = new Gson();

    public static final String PREFIX_SERVICE_PROVIDER = "ServiceProvider__TheRouter__";
    public static final String PREFIX_ROUTER_MAP = "RouterMap__TheRouter__";
    public static final String SUFFIX_AUTOWIRED_DOT_CLASS = "__TheRouter__Autowired.class";
    public static final String SUFFIX_AUTOWIRED = "__TheRouter__Autowired";
    public static final String FIELD_FLOW_TASK_JSON = "FLOW_TASK_JSON";
    public static final String FIELD_APT_VERSION = "THEROUTER_APT_VERSION";
    public static final String FIELD_ROUTER_MAP = "ROUTERMAP";
    public static final String FIELD_ROUTER_MAP_COUNT = "COUNT";
    public static final String UNKNOWN_VERSION = "unspecified";
    public static final String NOT_FOUND_VERSION = "0.0.0";
    public static final String DOT_CLASS = ".class";

    public static JarInfo fromCache(File cacheFile) throws IOException {
        String json = new String(Files.readAllBytes(cacheFile.toPath()), "UTF-8");
        return gson.fromJson(json, JarInfo.class);
    }

    public static void toCache(File cacheFile, JarInfo jarInfo) throws IOException {
        String json = gson.toJson(jarInfo);
        Files.write(cacheFile.toPath(), json.getBytes("UTF-8"));
    }

    /**
     * 标记当前jar中是否有要处理的类，生成类总共三种：RouterMap、ServiceProvider、Autowired
     * @param jarFile
     * @return
     */
    public static JarInfo tagJar(File jarFile, boolean isDebug) throws IOException {
        JarInfo jarInfo = new JarInfo();
        if (jarFile != null) {
            try (JarFile file = new JarFile(jarFile)) {
                Enumeration<JarEntry> enumeration = file.entries();
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = enumeration.nextElement();
                    String entryName = jarEntry.getName();
                    jarInfo.allJarClass.add(entryName.replace("/", "."));
                    if (entryName.contains(PREFIX_SERVICE_PROVIDER) && !entryName.contains("$")) {
                        int start = entryName.indexOf(PREFIX_SERVICE_PROVIDER);
                        int end = entryName.length() - DOT_CLASS.length();
                        String className = entryName.substring(start, end)
                                .replace('\\', '.')
                                .replace('/', '.');
                        if (className.indexOf('$') <= 0) { // 只处理非内部类
                            try (InputStream inputStream = file.getInputStream(jarEntry)) {
                                ClassReader reader = new ClassReader(inputStream);
                                ClassNode cn = new ClassNode();
                                reader.accept(cn, 0);
                                List<FieldNode> fieldList = cn.fields;
                                String aptVersion = NOT_FOUND_VERSION;
                                for (FieldNode fieldNode : fieldList) {
                                    if (FIELD_FLOW_TASK_JSON.equals(fieldNode.name)) {
                                        System.out.println("---------TheRouter in jar get flow task json from: " + entryName + "-------------------------------");
                                        Map<String, String> map = gson.fromJson((String) fieldNode.value, new TypeToken<Map<String, String>>(){}.getType());
                                        jarInfo.flowTaskMapFromJar.putAll(map);
                                    } else if (FIELD_APT_VERSION.equals(fieldNode.name)) {
                                        aptVersion = (String) fieldNode.value;
                                    }
                                }
                                if (!serviceProvideMap.containsKey(className) || !NOT_FOUND_VERSION.equals(aptVersion)) {
                                    serviceProvideMap.put(className, aptVersion);
                                }
                            }
                        }
                    } else if (entryName.contains("TheRouterServiceProvideInjecter") && !entryName.contains("$")) {
                        jarInfo.isTheRouterJar = true;
                        jarInfo.theRouterInjectEntryName = entryName;
                    } else if (entryName.contains(SUFFIX_AUTOWIRED_DOT_CLASS) && !entryName.contains("$")) {
                        String className = entryName
                                .replace(DOT_CLASS, "")
                                .replace('\\', '.')
                                .replace('/', '.');
                        autowiredSet.add(className);
                    } else if (entryName.contains(PREFIX_ROUTER_MAP) && !entryName.contains("$")) {
                        routeSet.add(entryName);
                        try (InputStream inputStream = file.getInputStream(jarEntry)) {
                            ClassReader reader = new ClassReader(inputStream);
                            ClassNode cn = new ClassNode();
                            reader.accept(cn, 0);
                            Map<String, String> fieldMap = new HashMap<>();
                            int count = 0;
                            List<FieldNode> fieldList = cn.fields;
                            for (FieldNode fieldNode : fieldList) {
                                if (FIELD_ROUTER_MAP_COUNT.equals(fieldNode.name)) {
                                    count = (int) fieldNode.value;
                                }
                                if (fieldNode.name.startsWith(FIELD_ROUTER_MAP)) {
                                    fieldMap.put(fieldNode.name, (String) fieldNode.value);
                                }
                            }

                            if (fieldMap.size() == 1 && count == 0) {  // old version
                                for (String value : fieldMap.values()) {
                                    System.out.println("---------TheRouter in jar get route map from: " + entryName + "-------------------------------");
                                    if (isDebug) {
                                        System.out.println(value);
                                    }
                                    jarInfo.routeMapStringFromJar.add(value);
                                }
                            } else if (fieldMap.size() == count) {  // new version
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < count; i++) {
                                    stringBuilder.append(fieldMap.get(FIELD_ROUTER_MAP + i));
                                }
                                System.out.println("---------TheRouter in jar get route map from: " + entryName + "-------------------------------");
                                String route = stringBuilder.toString();
                                if (isDebug) {
                                    System.out.println(route);
                                }
                                jarInfo.routeMapStringFromJar.add(route);
                            }
                        }
                    }
                }
            }
        }
        return jarInfo;
    }

    /**
     * 本方法仅 Transform API 会用到
     */
    public static SourceInfo tagClass(String path, boolean isDebug) throws IOException {
        SourceInfo sourceInfo = new SourceInfo();
        File dir = new File(path);
        if (dir.isDirectory()) {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filePath = file.toString();
                    sourceInfo.allSourceClass.add(filePath.replace(File.separator, "."));
                    if (filePath.contains(PREFIX_SERVICE_PROVIDER) && !filePath.contains("$")) {
                        int start = filePath.indexOf(PREFIX_SERVICE_PROVIDER);
                        int end = filePath.length() - DOT_CLASS.length();
                        String className = filePath.substring(start, end)
                                .replace(File.separator, ".");
                        if (className.indexOf('$') > 0) {
                            className = className.substring(0, className.indexOf('$'));
                        }
                        try (FileInputStream inputStream = new FileInputStream(file.toFile())) {
                            ClassReader reader = new ClassReader(inputStream);
                            ClassNode cn = new ClassNode();
                            reader.accept(cn, 0);
                            List<FieldNode> fieldList = cn.fields;
                            String aptVersion = NOT_FOUND_VERSION;
                            for (FieldNode fieldNode : fieldList) {
                                if (FIELD_FLOW_TASK_JSON.equals(fieldNode.name)) {
                                    System.out.println("---------TheRouter in source get flow task json from: " + file.getFileName() + "-------------------------------");
                                    Map<String, String> map = gson.fromJson((String) fieldNode.value, new TypeToken<Map<String, String>>(){}.getType());
                                    sourceInfo.flowTaskMapFromSource.putAll(map);
                                } else if (FIELD_APT_VERSION.equals(fieldNode.name)) {
                                    aptVersion = (String) fieldNode.value;
                                }
                            }
                            if (!serviceProvideMap.containsKey(className) || !NOT_FOUND_VERSION.equals(aptVersion)) {
                                serviceProvideMap.put(className, aptVersion);
                            }
                        }
                    } else if (filePath.contains(SUFFIX_AUTOWIRED_DOT_CLASS) && !filePath.contains("$")) {
                        String className = filePath
                                .replace(path, "")
                                .replace(DOT_CLASS, "")
                                .replace(File.separator, ".")
                                .replace("classes.", "");
                        if (className.startsWith(".")) {
                            className = className.substring(1);
                        }
                        autowiredSet.add(className);
                    } else if (filePath.contains(PREFIX_ROUTER_MAP) && !filePath.contains("$")) {
                        int start = filePath.indexOf(PREFIX_ROUTER_MAP);
                        int end = filePath.length() - DOT_CLASS.length();
                        String className = filePath.substring(start, end)
                                .replace(File.separator, ".");
                        // 因为absolutePath过滤的时候是直接以类名过滤，就把包名去掉了
                        // 包名一定是a，所以这里补回来
                        routeSet.add("a/" + className);

                        try (FileInputStream inputStream = new FileInputStream(file.toFile())) {
                            ClassReader reader = new ClassReader(inputStream);
                            ClassNode cn = new ClassNode();
                            reader.accept(cn, 0);

                            Map<String, String> fieldMap = new HashMap<>();
                            int count = 0;
                            List<FieldNode> fieldList = cn.fields;
                            for (FieldNode fieldNode : fieldList) {
                                if (FIELD_ROUTER_MAP_COUNT.equals(fieldNode.name)) {
                                    count = (int) fieldNode.value;
                                }
                                if (fieldNode.name.startsWith(FIELD_ROUTER_MAP)) {
                                    fieldMap.put(fieldNode.name, (String) fieldNode.value);
                                }
                            }

                            if (fieldMap.size() == 1 && count == 0) {  // old version
                                for (String value : fieldMap.values()) {
                                    System.out.println("---------TheRouter in source get route map from: " + file.getFileName() + "-------------------------------");
                                    if (isDebug) {
                                        System.out.println(value);
                                    }
                                    sourceInfo.routeMapStringFromSource.add(value);
                                }
                            } else if (fieldMap.size() == count) {  // new version
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < count; i++) {
                                    stringBuilder.append(fieldMap.get(FIELD_ROUTER_MAP + i));
                                }
                                System.out.println("---------TheRouter in source get route map from: " + file.getFileName() + "-------------------------------");
                                String route = stringBuilder.toString();
                                if (isDebug) {
                                    System.out.println(route);
                                }
                                sourceInfo.routeMapStringFromSource.add(route);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return sourceInfo;
    }

    /**
     * 开始修改 TheRouterServiceProvideInjecter 类
     */
    static void injectClassCode(File inputJarFile) throws IOException {
        long start = System.currentTimeMillis();
        File optJarFile = new File(inputJarFile.getParent(), inputJarFile.getName() + ".opt");
        try (JarFile inputJar = new JarFile(inputJarFile);
             JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJarFile))) {

            Enumeration<JarEntry> enumeration = inputJar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                jarOutputStream.putNextEntry(zipEntry);

                byte[] bytes;
                if (entryName.contains("TheRouterServiceProvideInjecter")) {
                    try (InputStream inputStream = inputJar.getInputStream(jarEntry)) {
                        ClassReader cr = new ClassReader(inputStream);
                        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                        AddCodeVisitor cv = new AddCodeVisitor(cw, serviceProvideMap, autowiredSet, routeSet, false);
                        cr.accept(cv, ClassReader.SKIP_DEBUG);
                        bytes = cw.toByteArray();
                    }
                } else {
                    try (InputStream inputStream = inputJar.getInputStream(jarEntry)) {
                        bytes = toByteArray(inputStream);
                    }
                }
                jarOutputStream.write(bytes);
                jarOutputStream.closeEntry();
            }
        }

        // 替换原文件
        if (!inputJarFile.delete()) {
            throw new IOException("Failed to delete original jar: " + inputJarFile);
        }
        if (!optJarFile.renameTo(inputJarFile)) {
            throw new IOException("Failed to rename opt jar to original: " + optJarFile);
        }

        long time = System.currentTimeMillis() - start;
        System.out.println("---------TheRouter inject TheRouterServiceProvideInjecter.class, spend：" + time + "ms----------------------");
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }
}