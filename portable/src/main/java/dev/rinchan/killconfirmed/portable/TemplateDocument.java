package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TemplateDocument {
    private final List<Line> lines;

    private TemplateDocument(List<Line> lines) { this.lines = List.copyOf(lines); }

    public static TemplateDocument parse(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) throw new TemplateException("Lore template root must be an array");
            List<Line> lines = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) throw new TemplateException("Every lore template line must be an object");
                JsonObject object = element.getAsJsonObject();
                if (!object.has("component")) throw new TemplateException("Lore template line is missing component");
                Optional<PlaceholderKey> condition = Optional.empty();
                if (object.has("when")) condition = Optional.of(PlaceholderKey.parse(object.get("when").getAsString()));
                lines.add(new Line(condition, object.get("component").deepCopy()));
            }
            return new TemplateDocument(lines);
        } catch (JsonParseException | IllegalStateException exception) {
            throw new TemplateException("Malformed lore template JSON", exception);
        }
    }

    public List<JsonElement> render(Map<PlaceholderKey, JsonElement> values) {
        List<JsonElement> rendered = new ArrayList<>();
        for (Line line : lines) {
            if (line.condition().isPresent() && !values.containsKey(line.condition().get())) continue;
            rendered.add(resolve(line.component(), values));
        }
        return List.copyOf(rendered);
    }

    private static JsonElement resolve(JsonElement node, Map<PlaceholderKey, JsonElement> values) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            if (object.size() == 1 && object.has("placeholder")) {
                PlaceholderKey key = PlaceholderKey.parse(object.get("placeholder").getAsString());
                JsonElement value = values.get(key);
                if (value == null) throw new TemplateException("Missing placeholder: " + key);
                return value.deepCopy();
            }
            JsonObject copy = new JsonObject();
            for (var entry : object.entrySet()) copy.add(entry.getKey(), resolve(entry.getValue(), values));
            return copy;
        }
        if (node.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : node.getAsJsonArray()) copy.add(resolve(child, values));
            return copy;
        }
        return node.deepCopy();
    }

    private record Line(Optional<PlaceholderKey> condition, JsonElement component) {}
}
