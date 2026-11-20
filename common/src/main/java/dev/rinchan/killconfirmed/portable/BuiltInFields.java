package dev.rinchan.killconfirmed.portable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltInFields {
    public static final String NAMESPACE = "kill_confirmed";

    private BuiltInFields() {}

    public static Map<PlaceholderKey, JsonElement> from(SnapshotFields fields) {
        Map<PlaceholderKey, JsonElement> values = new LinkedHashMap<>();
        values.put(key("player"), fields.owner().deepCopy());
        fields.killer().ifPresent(killer -> values.put(key("killer"), killer.deepCopy()));
        fields.weapon().ifPresent(weapon -> values.put(key("weapon"), weapon.deepCopy()));
        values.put(key("death_message"), fields.deathMessage().deepCopy());
        values.put(key("location"), fields.locationDisplay().deepCopy());
        values.put(key("x"), literal(Integer.toString(fields.x())));
        values.put(key("y"), literal(Integer.toString(fields.y())));
        values.put(key("z"), literal(Integer.toString(fields.z())));
        values.put(key("dimension"), fields.dimensionDisplay().deepCopy());
        values.put(key("level"), literal(Integer.toString(fields.level())));
        values.put(key("day"), literal(Long.toString(fields.day())));
        return Collections.unmodifiableMap(values);
    }

    private static PlaceholderKey key(String path) { return new PlaceholderKey(NAMESPACE, path); }

    private static JsonObject literal(String text) {
        JsonObject component = new JsonObject();
        component.addProperty("text", text);
        return component;
    }
}
