package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;
import dev.rinchan.killconfirmed.KillConfirmedConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    @TempDir Path directory;

    @Test
    void creates_commented_toml_defaults_once() throws Exception {
        var config = KillConfirmedConfig.load(directory);
        var file = directory.resolve("kill_confirmed.toml");
        assertTrue(Files.exists(file), "first launch creates the TOML configuration");
        assertEquals(DropStrategy.AT_DEATH_POSITION, config.dropStrategy());
        assertTrue(Files.readString(file).startsWith("# Where the dog tag goes:"));
        var original = Files.readString(file);
        KillConfirmedConfig.load(directory);
        assertEquals(original, Files.readString(file));
        assertFalse(Files.exists(directory.resolve("kill_confirmed.json")));
    }

    @Test
    void reads_toml_and_does_not_migrate_old_json() throws Exception {
        Files.writeString(directory.resolve("kill_confirmed.json"), "invalid old config");
        Files.writeString(directory.resolve("kill_confirmed.toml"), """
                drop = "respawn_inventory"
                player_kills_only = true
                function = "example:death"
                lore = ['{"text":"Custom","italic":false}']
                """);
        var config = KillConfirmedConfig.load(directory);
        assertEquals(DropStrategy.ON_RESPAWN_INVENTORY, config.dropStrategy());
        assertTrue(config.playerKillsOnly());
        assertEquals("example:death", config.function());
        var result = config.loreTemplate().render(java.util.Map.of());
        assertEquals("Custom", result.getFirst().getAsJsonObject().get("text").getAsString());
        assertEquals("invalid old config", Files.readString(directory.resolve("kill_confirmed.json")));
    }
    @Test
    void omitted_settings_keep_defaults_and_invalid_settings_fail_at_load() throws Exception {
        var file = directory.resolve("kill_confirmed.toml");
        Files.writeString(file, "drop = \"respawn_inventory\"\n");
        var config = KillConfirmedConfig.load(directory);
        assertFalse(config.playerKillsOnly());
        assertEquals("", config.function());
        assertEquals(1, config.loreTemplate().render(java.util.Map.of(
                PlaceholderKey.parse("kill_confirmed:death_message"),
                com.google.gson.JsonParser.parseString("{\"text\":\"fell\"}"))).size());
        for (String invalid : java.util.List.of("drop = 'elsewhere'", "player_kills_only = 'yes'",
                "function = 'not an id'", "lore = [1]", "lore = 'bad'")) {
            Files.writeString(file, invalid);
            assertThrows(IllegalStateException.class, () -> KillConfirmedConfig.load(directory), invalid);
        }
    }

}
