package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FunctionOutputParser {
    public static final int MAX_ENTRIES = 32;
    public static final int MAX_COMPONENT_BYTES = 8192;
    public static final int MAX_TOTAL_BYTES = 32768;

    private FunctionOutputParser() {}

    public static FunctionParseResult parse(List<FunctionOutput> outputs) {
        if (outputs.size() > MAX_ENTRIES) {
            return failed("Too many function placeholder outputs: " + outputs.size() + " > " + MAX_ENTRIES);
        }
        int totalBytes = outputs.stream().mapToInt(output -> bytes(output.componentJson())).sum();
        if (totalBytes > MAX_TOTAL_BYTES) return failed("Function placeholder output is oversized in total: " + totalBytes);

        Map<PlaceholderKey, JsonElement> values = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        for (FunctionOutput output : outputs) {
            PlaceholderKey key;
            try {
                key = PlaceholderKey.parse(output.id());
            } catch (RuntimeException exception) {
                errors.add("Malformed placeholder id: " + output.id());
                continue;
            }
            if (BuiltInFields.NAMESPACE.equals(key.namespace())) {
                errors.add("Function output cannot use built-in namespace: " + key);
                continue;
            }
            if (values.containsKey(key)) {
                errors.add("Duplicate function placeholder output: " + key);
                values.remove(key);
                continue;
            }
            int componentBytes = bytes(output.componentJson());
            if (componentBytes > MAX_COMPONENT_BYTES) {
                errors.add("Oversized component output for " + key + ": " + componentBytes);
                continue;
            }
            try {
                JsonElement component = JsonParser.parseString(output.componentJson());
                if (component.isJsonNull()) throw new JsonParseException("null is not a component");
                values.put(key, component);
            } catch (JsonParseException exception) {
                errors.add("Malformed component output for " + key + ": " + exception.getMessage());
            }
        }
        if (!errors.isEmpty()) values.clear();
        return new FunctionParseResult(Collections.unmodifiableMap(values), List.copyOf(errors));
    }

    private static int bytes(String value) {
        return value == null ? Integer.MAX_VALUE : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static FunctionParseResult failed(String error) {
        return new FunctionParseResult(Map.of(), List.of(error));
    }
}
