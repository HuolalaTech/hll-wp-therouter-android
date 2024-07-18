package com.therouter.plugin;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JarInfo implements Serializable {
    public boolean isTheRouterJar;
    public final Set<String> allJarClass = new HashSet<>();
    public final Set<String> routeMapStringFromJar = new HashSet<>();
    public final Map<String, String> flowTaskMapFromJar = new HashMap<>();
}
