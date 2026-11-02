package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateEngineTest {
    @Test
    void substitutes_nested_components_and_preserves_placeholder_style() {
        var template = TemplateDocument.parse("""
                [{"translate":"example.line","italic":false,"with":[{"placeholder":"player","color":"#E2C88C","bold":false}]}]
                """);
        var value = JsonParser.parseString("{\"text\":\"Alice\",\"color\":\"red\",\"bold\":true,\"extra\":[{\"text\":\"!\"}]}");
        var result = template.render(Map.of(new PlaceholderKey("kill_confirmed", "player"), value));
        assertEquals(JsonParser.parseString("""
                [{"translate":"example.line","italic":false,"with":[{"text":"Alice","color":"#E2C88C","bold":false,"extra":[{"text":"!"}]}]}]
                """), new com.google.gson.Gson().toJsonTree(result));
        assertEquals("red", value.getAsJsonObject().get("color").getAsString());
    }

    @Test
    void hides_the_whole_line_when_any_nested_placeholder_is_missing() {
        var template = TemplateDocument.parse("""
                [{"text":"Killer: ","extra":[{"placeholder":"killer"}]},{"text":"Still here","italic":false}]
                """);
        var result = template.render(Map.of());
        assertEquals(1, result.size());
        assertEquals("Still here", result.getFirst().getAsJsonObject().get("text").getAsString());
    }

    @Test
    void supports_namespaced_java_providers_without_flattening_components() {
        var template = TemplateDocument.parse("""
                [{"placeholder":"example:rank","italic":false}]
                """);
        var value = JsonParser.parseString("{\"translate\":\"example.rank\",\"bold\":true}");
        var result = template.render(Map.of(PlaceholderKey.parse("example:rank"), value));
        assertEquals(JsonParser.parseString("{\"translate\":\"example.rank\",\"bold\":true,\"italic\":false}"), result.getFirst());
    }
    @Test
    void skips_bad_json_and_native_parse_failures_and_warns_once_per_line() {
        var template = new TemplateDocument(java.util.List.of("{", "{\"text\":\"bad native\"}", "{\"text\":\"good\"}"));
        var warnings = new java.util.ArrayList<Integer>();
        TemplateDocument.ComponentParser<String> parser = json -> {
            var text = json.getAsJsonObject().get("text").getAsString();
            if (text.equals("bad native")) throw new IllegalArgumentException("Rejected by component codec");
            return text;
        };
        assertEquals(java.util.List.of("good"), template.render(Map.of(), parser, (line, failure) -> warnings.add(line)));
        assertEquals(java.util.List.of(1, 2), warnings);
        assertEquals(java.util.List.of("good"), template.render(Map.of(), parser, (line, failure) -> warnings.add(line)));
        assertEquals(java.util.List.of(1, 2), warnings);
    }

}
