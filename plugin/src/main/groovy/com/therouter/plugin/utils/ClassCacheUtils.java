package com.therouter.plugin.utils;

import com.therouter.plugin.BuildConfig;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassCacheUtils {

    public static void write(Set<String> set, File file) throws IOException {
        String content = set2String(set);
        if (file.exists()) {
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        ResourceGroovyMethods.write(file, content);
    }

    public static Set<String> readToSet(File file) throws IOException {
        Set<String> set = new HashSet<>();
        if (file.exists()) {
            List<String> list = ResourceGroovyMethods.readLines(file);
            for (String str : list) {
                if (!str.isBlank()) {
                    set.add(str);
                }
            }
        }
        return set;
    }

    public static Map<String, String> readToMap(File file) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (file.exists()) {
            List<String> list = ResourceGroovyMethods.readLines(file);
            for (String str : list) {
                if (!str.isBlank()) {
                    map.put(str, BuildConfig.VERSION);
                }
            }
        }
        return map;
    }

    private static String set2String(Set<String> set) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : set) {
            stringBuilder.append(str).append("\n");
        }
        return stringBuilder.toString();
    }
}
