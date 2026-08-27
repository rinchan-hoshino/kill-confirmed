package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuiltInFieldsTest {
    @Test
    void exposes_the_documented_default_fields_as_structured_components() {
        var fields = new SnapshotFields(
                JsonParser.parseString("{\"text\":\"Alice\",\"color\":\"aqua\"}"),
                Optional.of(JsonParser.parseString("{\"translate\":\"entity.minecraft.zombie\"}")),
                JsonParser.parseString("{\"translate\":\"death.attack.mob\",\"with\":[{\"text\":\"Alice\"}]}"),
                12, 64, -8,
                "minecraft:overworld",
                JsonParser.parseString("{\"translate\":\"dimension.minecraft.overworld\"}"),
                27);

        var values = BuiltInFields.from(fields);

        assertEquals(Set.of("owner", "killer", "death_message", "coordinates", "dimension", "dimension_id", "level"),
                values.keySet().stream().map(PlaceholderKey::path).collect(java.util.stream.Collectors.toSet()));
        assertEquals("kill_confirmed", values.keySet().iterator().next().namespace());
        assertTrue(values.get(PlaceholderKey.parse("kill_confirmed:coordinates")).toString().contains("12"));
        assertTrue(values.get(PlaceholderKey.parse("kill_confirmed:dimension")).toString().contains("dimension.minecraft.overworld"));
        assertEquals(27, values.get(PlaceholderKey.parse("kill_confirmed:level")).getAsJsonObject()
                .getAsJsonArray("with").get(0).getAsInt());
    }

    @Test
    void omits_killer_only_when_absent() {
        var fields = new SnapshotFields(
                JsonParser.parseString("\"Alice\""), Optional.empty(), JsonParser.parseString("\"fell\""),
                0, 70, 0, "minecraft:overworld", JsonParser.parseString("\"Overworld\""), 0);
        assertFalse(BuiltInFields.from(fields).containsKey(PlaceholderKey.parse("kill_confirmed:killer")));
    }
}
