package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BuiltInFieldsTest {
    private SnapshotFields fields(boolean killer) {
        return new SnapshotFields(
                JsonParser.parseString("\"Alice\""),
                killer ? Optional.of(JsonParser.parseString("{\"translate\":\"entity.minecraft.zombie\"}")) : Optional.empty(),
                killer ? Optional.of(JsonParser.parseString("{\"text\":\"Named Sword\",\"italic\":true}")) : Optional.empty(),
                JsonParser.parseString("{\"translate\":\"death.attack.mob\",\"with\":[\"Alice\"]}"),
                JsonParser.parseString("{\"translate\":\"structure.minecraft.ancient_city\",\"fallback\":\"Ancient City\"}"),
                12, -64, -8, "minecraft:overworld",
                JsonParser.parseString("{\"translate\":\"kill_confirmed.dimension.minecraft.overworld\"}"),
                27, 152);
    }

    @Test
    void exposes_native_components_location_and_integer_values() {
        var values = BuiltInFields.from(fields(true));
        assertEquals(
                Set.of("player", "killer", "weapon", "death_message", "location", "x", "y", "z", "dimension", "level", "day"),
                values.keySet().stream().map(PlaceholderKey::path).collect(Collectors.toSet()));
        assertEquals("-64", values.get(PlaceholderKey.parse("kill_confirmed:y")).getAsJsonObject().get("text").getAsString());
        assertEquals("27", values.get(PlaceholderKey.parse("kill_confirmed:level")).getAsJsonObject().get("text").getAsString());
        assertEquals("152", values.get(PlaceholderKey.parse("kill_confirmed:day")).getAsJsonObject().get("text").getAsString());
        assertEquals("structure.minecraft.ancient_city",
                values.get(PlaceholderKey.parse("kill_confirmed:location")).getAsJsonObject().get("translate").getAsString());
        assertEquals("entity.minecraft.zombie",
                values.get(PlaceholderKey.parse("kill_confirmed:killer")).getAsJsonObject().get("translate").getAsString());
        assertTrue(values.get(PlaceholderKey.parse("kill_confirmed:weapon")).getAsJsonObject().get("italic").getAsBoolean());
    }

    @Test
    void omits_unavailable_killer_and_weapon() {
        var values = BuiltInFields.from(fields(false));
        assertFalse(values.containsKey(PlaceholderKey.parse("kill_confirmed:killer")));
        assertFalse(values.containsKey(PlaceholderKey.parse("kill_confirmed:weapon")));
        assertTrue(values.containsKey(PlaceholderKey.parse("kill_confirmed:location")));
    }
}
