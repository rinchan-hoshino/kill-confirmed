package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltInFields {
    public static final String NAMESPACE = "kill_confirmed";
    private BuiltInFields() {}

    public static Map<PlaceholderKey, JsonElement> from(SnapshotFields fields) {
        Map<PlaceholderKey, JsonElement> values = new LinkedHashMap<>();
        values.put(key("owner"), fields.owner().deepCopy());
        fields.killer().ifPresent(killer -> values.put(key("killer"), killer.deepCopy()));
        values.put(key("death_message"), fields.deathMessage().deepCopy());
        values.put(key("coordinates"), translated("tooltip.kill_confirmed.coordinates.value",
                new JsonPrimitive(fields.x()), new JsonPrimitive(fields.y()), new JsonPrimitive(fields.z())));
        values.put(key("dimension"), fields.dimensionDisplay().deepCopy());
        JsonObject dimensionId = new JsonObject();
        dimensionId.addProperty("text", fields.dimensionId());
        values.put(key("dimension_id"), dimensionId);
        values.put(key("level"), translated("tooltip.kill_confirmed.level.value", new JsonPrimitive(fields.level())));
        return Collections.unmodifiableMap(values);
    }

    private static PlaceholderKey key(String path) { return new PlaceholderKey(NAMESPACE, path); }

    private static JsonObject translated(String key, JsonElement... arguments) {
        JsonObject component = new JsonObject();
        component.addProperty("translate", key);
        JsonArray with = new JsonArray();
        for (JsonElement argument : arguments) with.add(argument);
        component.add("with", with);
        return component;
    }
}
