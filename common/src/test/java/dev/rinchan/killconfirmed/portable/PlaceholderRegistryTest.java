package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaceholderRegistryTest {
    @Test
    void rejects_builtin_namespace_and_duplicate_custom_keys() {
        var registry = new PlaceholderRegistry<String>();
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                PlaceholderKey.parse("kill_confirmed:owner"), context -> Optional.empty()));
        registry.register(PlaceholderKey.parse("addon:rank"), context -> Optional.of(JsonParser.parseString("\"A\"")));
        assertThrows(IllegalStateException.class, () -> registry.register(
                PlaceholderKey.parse("addon:rank"), context -> Optional.empty()));
    }

    @Test
    void isolates_provider_failure_and_reports_it_without_silent_loss() {
        var registry = new PlaceholderRegistry<String>();
        registry.register(PlaceholderKey.parse("addon:good"), context -> Optional.of(JsonParser.parseString("\"ok\"")));
        registry.register(PlaceholderKey.parse("addon:broken"), context -> { throw new IllegalStateException("boom"); });
        var failures = new java.util.ArrayList<String>();

        Map<PlaceholderKey, com.google.gson.JsonElement> values = registry.resolve("context", failures::add);

        assertEquals("ok", values.get(PlaceholderKey.parse("addon:good")).getAsString());
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().contains("addon:broken"));
        assertTrue(failures.getFirst().contains("boom"));
    }
}
