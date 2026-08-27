package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FunctionOutputParserTest {
    @Test
    void accepts_bounded_namespaced_component_json() {
        var result = FunctionOutputParser.parse(List.of(
                new FunctionOutput("addon:rank", "{\"text\":\"S\",\"color\":\"gold\"}")));
        assertTrue(result.errors().isEmpty());
        assertEquals("gold", result.values().get(PlaceholderKey.parse("addon:rank"))
                .getAsJsonObject().get("color").getAsString());
    }

    @Test
    void rejects_malformed_oversized_duplicate_and_builtin_outputs_explicitly() {
        var result = FunctionOutputParser.parse(List.of(
                new FunctionOutput("bad id", "{}"),
                new FunctionOutput("addon:broken", "{"),
                new FunctionOutput("addon:huge", "{\"text\":\"" + "x".repeat(FunctionOutputParser.MAX_COMPONENT_BYTES) + "\"}"),
                new FunctionOutput("kill_confirmed:owner", "\"hijack\""),
                new FunctionOutput("addon:dup", "\"one\""),
                new FunctionOutput("addon:dup", "\"two\"")));

        assertTrue(result.values().isEmpty());
        assertEquals(5, result.errors().size());
        assertTrue(result.errors().stream().anyMatch(error -> error.toLowerCase().contains("malformed")));
        assertTrue(result.errors().stream().anyMatch(error -> error.toLowerCase().contains("oversized")));
        assertTrue(result.errors().stream().anyMatch(error -> error.toLowerCase().contains("built-in namespace")));
        assertTrue(result.errors().stream().anyMatch(error -> error.toLowerCase().contains("duplicate")));
    }

    @Test
    void caps_entry_count() {
        var entries = java.util.stream.IntStream.range(0, FunctionOutputParser.MAX_ENTRIES + 1)
                .mapToObj(i -> new FunctionOutput("addon:k" + i, "\"v\""))
                .toList();
        var result = FunctionOutputParser.parse(entries);
        assertTrue(result.values().isEmpty());
        assertEquals(1, result.errors().size());
    }
}
