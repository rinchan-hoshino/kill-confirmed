package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateEngineTest {
    @Test
    void substitutes_components_without_flattening_native_formatting() {
        var template = TemplateDocument.parse("""
                [{"component":{"translate":"tooltip.example","color":"gold","with":[{"placeholder":"example:value"}]}}]
                """);
        var value = JsonParser.parseString("{\"translate\":\"example.value\",\"bold\":true}");

        var rendered = template.render(Map.of(PlaceholderKey.parse("example:value"), value));

        assertEquals(1, rendered.size());
        assertEquals("tooltip.example", rendered.getFirst().getAsJsonObject().get("translate").getAsString());
        assertEquals("gold", rendered.getFirst().getAsJsonObject().get("color").getAsString());
        assertTrue(rendered.getFirst().toString().contains("example.value"));
        assertTrue(rendered.getFirst().toString().contains("\"bold\":true"));
    }

    @Test
    void skips_a_conditionally_unavailable_killer_line_cleanly() {
        var template = TemplateDocument.parse("""
                [
                  {"when":"kill_confirmed:killer","component":{"translate":"tooltip.kill_confirmed.killer","with":[{"placeholder":"kill_confirmed:killer"}]}},
                  {"component":{"placeholder":"kill_confirmed:death_message"}}
                ]
                """);
        var values = Map.of(
                PlaceholderKey.parse("kill_confirmed:death_message"),
                JsonParser.parseString("{\"translate\":\"death.attack.fall\"}"));

        var rendered = template.render(values);

        assertEquals(1, rendered.size());
        assertEquals("death.attack.fall", rendered.getFirst().getAsJsonObject().get("translate").getAsString());
    }

    @Test
    void fails_explicitly_for_an_unconditional_missing_placeholder() {
        var template = TemplateDocument.parse("[{\"component\":{\"placeholder\":\"example:missing\"}}]");
        var failure = assertThrows(TemplateException.class, () -> template.render(Map.of()));
        assertTrue(failure.getMessage().contains("example:missing"));
    }
}
