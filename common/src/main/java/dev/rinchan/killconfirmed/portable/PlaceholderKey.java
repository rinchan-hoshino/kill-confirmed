package dev.rinchan.killconfirmed.portable;

import java.util.Objects;
import java.util.regex.Pattern;

public record PlaceholderKey(String namespace, String path) implements Comparable<PlaceholderKey> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public PlaceholderKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid namespaced placeholder: " + namespace + ":" + path);
        }
    }

    public static PlaceholderKey parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Invalid namespaced placeholder: " + value);
        }
        return new PlaceholderKey(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public String toString() { return namespace + ":" + path; }

    @Override
    public int compareTo(PlaceholderKey other) { return toString().compareTo(other.toString()); }
}
