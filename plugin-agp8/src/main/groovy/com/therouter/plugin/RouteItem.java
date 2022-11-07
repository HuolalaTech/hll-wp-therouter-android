package com.therouter.plugin;

import java.util.HashMap;
import java.util.Objects;

public class RouteItem implements Comparable<RouteItem> {
    String path = "";
    String className = "";
    String action = "";
    String description = "";
    HashMap<String, String> params = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteItem)) return false;
        RouteItem routeItem = (RouteItem) o;
        return Objects.equals(path, routeItem.path) &&
                Objects.equals(className, routeItem.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, className, action, description, params);
    }

    @Override
    public int compareTo(RouteItem routeItem) {
        if (routeItem == null || routeItem.className == null || className == null) {
            return 0;
        }
        if (routeItem.className.compareTo(className) == 0) {
            if (routeItem.path == null) {
                throw new RuntimeException("TheRouter " + routeItem.className + "'s path is Null");
            }
            if (path == null) {
                throw new RuntimeException("TheRouter " + className + "'s path is Null");
            }
            return routeItem.path.compareTo(path);
        } else {
            return routeItem.className.compareTo(className);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "path='" + path + '\'' +
                ", className='" + className + '\'' +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", params=" + params +
                '}';
    }
}