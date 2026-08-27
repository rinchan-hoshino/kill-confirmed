package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class PlaceholderRegistry<C> {
    private final Map<PlaceholderKey, PlaceholderProvider<C>> providers = new TreeMap<>();

    public synchronized void register(PlaceholderKey key, PlaceholderProvider<C> provider) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(provider, "provider");
        if (BuiltInFields.NAMESPACE.equals(key.namespace())) {
            throw new IllegalArgumentException("The built-in namespace is reserved: " + key);
        }
        if (providers.putIfAbsent(key, provider) != null) {
            throw new IllegalStateException("Placeholder provider already registered: " + key);
        }
    }

    public synchronized Map<PlaceholderKey, JsonElement> resolve(C context, Consumer<String> failureReporter) {
        Map<PlaceholderKey, JsonElement> values = new TreeMap<>();
        providers.forEach((key, provider) -> {
            try {
                provider.resolve(context).ifPresent(value -> values.put(key, value.deepCopy()));
            } catch (Exception exception) {
                failureReporter.accept("Placeholder provider " + key + " failed: " + exception.getMessage());
            }
        });
        return Collections.unmodifiableMap(values);
    }
}
