package com.therouter.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SourceInfo {
    public final Set<String> allSourceClass = new HashSet<>();
    public final Set<String> routeMapStringFromSource = new HashSet<>();
    public final Map<String, String> flowTaskMapFromSource = new HashMap<>();
}
