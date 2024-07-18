package com.therouter.plugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Node {
    private final String name;
    private Set<Node> children = new HashSet<>();

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<Node> getChildren() {
        return children;
    }

    public void setChildren(Set<Node> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(getName(), node.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return name;
    }
}
