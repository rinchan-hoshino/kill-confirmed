package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/** Resolves each lore line independently; unavailable values hide their entire line. */
public final class TemplateDocument {
    private final List<String> lines;
    private final Set<Integer> warnedLines = new HashSet<>();

    public TemplateDocument(List<String> lines) { this.lines = List.copyOf(lines); }

    public static TemplateDocument parse(String json) {
        var root = JsonParser.parseString(json).getAsJsonArray();
        return new TemplateDocument(root.asList().stream().map(JsonElement::toString).toList());
    }

    public List<JsonElement> render(Map<PlaceholderKey, JsonElement> values) {
        return render(values, component -> component, (line, failure) -> {});
    }

    @FunctionalInterface
    public interface ComponentParser<T> { T parse(JsonElement component) throws Exception; }

    public <T> List<T> render(Map<PlaceholderKey, JsonElement> values, ComponentParser<T> parser,
            BiConsumer<Integer, Exception> failureReporter) {
        List<T> rendered = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            try {
                rendered.add(parser.parse(resolve(JsonParser.parseString(lines.get(index)), values)));
            } catch (MissingPlaceholder ignored) {
                // A missing optional value hides this line, including all its labels.
            } catch (Exception failure) {
                if (warnedLines.add(index)) failureReporter.accept(index + 1, failure);
            }
        }
        return List.copyOf(rendered);
    }

    private static JsonElement resolve(JsonElement node, Map<PlaceholderKey, JsonElement> values) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            JsonObject copy = new JsonObject();
            if (object.has("placeholder")) {
                String name = object.get("placeholder").getAsString();
                PlaceholderKey key = name.contains(":") ? PlaceholderKey.parse(name)
                        : new PlaceholderKey(BuiltInFields.NAMESPACE, name);
                JsonElement value = values.get(key);
                if (value == null) throw new MissingPlaceholder();
                if (value.isJsonObject()) copy = value.deepCopy().getAsJsonObject();
                else {
                    copy.addProperty("text", "");
                    JsonArray extra = new JsonArray();
                    extra.add(value.deepCopy());
                    copy.add("extra", extra);
                }
            }
            for (var entry : object.entrySet()) {
                if (!entry.getKey().equals("placeholder"))
                    copy.add(entry.getKey(), resolve(entry.getValue(), values));
            }
            return copy;
        }
        if (node.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : node.getAsJsonArray()) copy.add(resolve(child, values));
            return copy;
        }
        return node.deepCopy();
    }

    private static final class MissingPlaceholder extends RuntimeException {}
}
