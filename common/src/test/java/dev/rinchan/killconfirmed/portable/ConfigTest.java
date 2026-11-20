package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;
import dev.rinchan.killconfirmed.KillConfirmedConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
        assertTrue(config.fireResistant());
        assertEquals(0, config.cooldownSeconds());
        assertTrue(config.enableDogTagCaseRecipe());
        assertTrue(config.enableCaseLoot());
        assertTrue(config.enableWandererTrade());
        assertTrue(Files.readString(file).startsWith("# Where the dog tag goes:"));
        var original = Files.readString(file);
        KillConfirmedConfig.load(directory);
        assertEquals(original, Files.readString(file));
        assertFalse(Files.exists(directory.resolve("kill_confirmed.json")));
    }

    @Test
    void migrates_v120_toml_smoothly_with_v200_defaults() throws Exception {
        Files.writeString(directory.resolve("kill_confirmed.toml"), """
                drop = "respawn_inventory"
                player_kills_only = true
                function = "example:death"
                lore = ['{"text":"Custom","italic":false}']
                """);
        var config = KillConfirmedConfig.load(directory);
        assertEquals(DropStrategy.ON_RESPAWN_INVENTORY, config.dropStrategy());
        assertTrue(config.playerKillsOnly());
        assertTrue(config.fireResistant());
        assertEquals(0, config.cooldownSeconds());
        assertTrue(config.enableDogTagCaseRecipe());
        assertTrue(config.enableCaseLoot());
        assertTrue(config.enableWandererTrade());
        assertEquals("example:death", config.function());
        var result = config.loreTemplate().render(Map.of());
        assertEquals("Custom", result.getFirst().getAsJsonObject().get("text").getAsString());
    }

    @Test
    void save_and_reload_round_trips_all_v200_options() {
        var custom = new KillConfirmedConfig(
                DropStrategy.ON_RESPAWN_INVENTORY,
                true,
                false,
                45,
                false,
                false,
                true,
                "pack:on_death",
                KillConfirmedConfig.DEFAULT_LORE_LINES,
                new TemplateDocument(KillConfirmedConfig.DEFAULT_LORE_LINES));
        custom.save(directory);
        var reloaded = KillConfirmedConfig.load(directory);
        assertEquals(DropStrategy.ON_RESPAWN_INVENTORY, reloaded.dropStrategy());
        assertTrue(reloaded.playerKillsOnly());
        assertFalse(reloaded.fireResistant());
        assertEquals(45, reloaded.cooldownSeconds());
        assertFalse(reloaded.enableDogTagCaseRecipe());
        assertFalse(reloaded.enableCaseLoot());
        assertTrue(reloaded.enableWandererTrade());
        assertEquals("pack:on_death", reloaded.function());
        assertEquals(3, reloaded.loreLines().size());
    }

    @Test
    void omitted_settings_keep_defaults_and_invalid_settings_fail_at_load() throws Exception {
        var file = directory.resolve("kill_confirmed.toml");
        Files.writeString(file, "drop = \"respawn_inventory\"\n");
        var config = KillConfirmedConfig.load(directory);
        assertFalse(config.playerKillsOnly());
        assertEquals("", config.function());
        assertEquals(1, config.loreTemplate().render(Map.of(
                PlaceholderKey.parse("kill_confirmed:death_message"),
                com.google.gson.JsonParser.parseString("{\"text\":\"fell\"}"))).size());
        for (String invalid : List.of(
                "drop = 'elsewhere'",
                "player_kills_only = 'yes'",
                "cooldown_seconds = -5",
                "function = 'not an id'",
                "lore = [1]",
                "lore = 'bad'")) {
            Files.writeString(file, invalid);
            assertThrows(IllegalStateException.class, () -> KillConfirmedConfig.load(directory), invalid);
        }
    }
}
