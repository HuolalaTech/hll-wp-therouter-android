package com.therouter.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JarInfo {
    public final Set<String> allJarClass = new HashSet<>();
    boolean isTheRouterJar = false;
    public final Set<String> routeMapStringFromJar = new HashSet<>();
    public final Map<String, String> flowTaskMapFromJar = new HashMap<>();
}
