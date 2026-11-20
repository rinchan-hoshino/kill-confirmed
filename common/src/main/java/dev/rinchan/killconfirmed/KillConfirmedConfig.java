package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.killconfirmed.portable.SimpleToml;
import dev.rinchan.killconfirmed.portable.TemplateDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public record KillConfirmedConfig(
        DropStrategy dropStrategy,
        boolean playerKillsOnly,
        boolean fireResistant,
        int cooldownSeconds,
        boolean enableDogTagCaseRecipe,
        boolean enableCaseLoot,
        boolean enableWandererTrade,
        String function,
        List<String> loreLines,
        TemplateDocument loreTemplate) {

    public static final String FILE_NAME = "kill_confirmed.toml";

    public static final List<String> DEFAULT_LORE_LINES = List.of(
            "{\"placeholder\":\"death_message\",\"color\":\"#CCCCCC\",\"italic\":false}",
            "{\"text\":\"\",\"color\":\"#808080\",\"italic\":false,\"extra\":[{\"text\":\"⌖ \",\"color\":\"#808080\"},{\"placeholder\":\"location\",\"color\":\"#CCCCCC\"},{\"text\":\" · (\",\"color\":\"#808080\"},{\"placeholder\":\"x\",\"color\":\"#808080\"},{\"text\":\", \",\"color\":\"#808080\"},{\"placeholder\":\"y\",\"color\":\"#808080\"},{\"text\":\", \",\"color\":\"#808080\"},{\"placeholder\":\"z\",\"color\":\"#808080\"},{\"text\":\")\",\"color\":\"#808080\"}]}",
            "{\"text\":\"\",\"color\":\"#707070\",\"italic\":false,\"extra\":[{\"text\":\"✦ \",\"color\":\"#707070\"},{\"translate\":\"kill_confirmed.lore.level_day\",\"color\":\"#D8C088\",\"with\":[{\"placeholder\":\"level\",\"color\":\"#D8C088\"},{\"placeholder\":\"day\",\"color\":\"#D8C088\"}]}]}"
    );

    public static KillConfirmedConfig defaults() {
        return new KillConfirmedConfig(
                DropStrategy.AT_DEATH_POSITION,
                false,
                true,
                0,
                true,
                true,
                true,
                "",
                DEFAULT_LORE_LINES,
                new TemplateDocument(DEFAULT_LORE_LINES));
    }

    public static KillConfirmedConfig load(Path configDirectory) {
        Path file = configDirectory.resolve(FILE_NAME);
        try {
            String defaultText = loadDefaultText();
            if (Files.notExists(file)) {
                Files.createDirectories(configDirectory);
                Files.writeString(file, defaultText, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            Map<String, Object> builtIn = SimpleToml.parse(defaultText);
            Map<String, Object> config = SimpleToml.parse(Files.readString(file, StandardCharsets.UTF_8));

            String drop = getString(config, "drop", getString(builtIn, "drop", "death_position"));
            DropStrategy strategy = switch (drop) {
                case "death_position" -> DropStrategy.AT_DEATH_POSITION;
                case "respawn_inventory" -> DropStrategy.ON_RESPAWN_INVENTORY;
                default -> throw new IllegalArgumentException("Unknown drop strategy: " + drop);
            };

            boolean playerKillsOnly = getBoolean(config, "player_kills_only", false);
            boolean fireResistant = getBoolean(config, "fire_resistant", true);
            int cooldownSeconds = getInt(config, "cooldown_seconds", 0);
            if (cooldownSeconds < 0) {
                throw new IllegalArgumentException("cooldown_seconds must not be negative: " + cooldownSeconds);
            }
            boolean enableCaseRecipe = getBoolean(config, "enable_dog_tag_case_recipe", true);
            boolean enableCaseLoot = getBoolean(config, "enable_case_loot", true);
            boolean enableWandererTrade = getBoolean(config, "enable_wanderer_trade", true);

            String function = getString(config, "function", "");
            if (!function.isEmpty() && !function.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
                throw new IllegalArgumentException("Invalid function id: " + function);
            }

            List<String> loreLines = getStringList(config, "lore",
                    getStringList(builtIn, "lore", DEFAULT_LORE_LINES));

            return new KillConfirmedConfig(
                    strategy,
                    playerKillsOnly,
                    fireResistant,
                    cooldownSeconds,
                    enableCaseRecipe,
                    enableCaseLoot,
                    enableWandererTrade,
                    function,
                    loreLines,
                    new TemplateDocument(loreLines));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load Kill Confirmed config " + file + ": " + exception.getMessage(), exception);
        }
    }

    public void save(Path configDirectory) {
        Path file = configDirectory.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDirectory);
            Files.writeString(file, toToml(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot save Kill Confirmed config " + file + ": " + exception.getMessage(), exception);
        }
    }

    public String toToml() {
        String dropValue = dropStrategy == DropStrategy.ON_RESPAWN_INVENTORY
                ? "respawn_inventory"
                : "death_position";
        StringBuilder sb = new StringBuilder();
        sb.append("# Where the dog tag goes: \"death_position\" or \"respawn_inventory\"\n");
        sb.append("drop = ").append(SimpleToml.escapeString(dropValue)).append('\n');
        sb.append("# Only create a dog tag when another player is the killer\n");
        sb.append("player_kills_only = ").append(playerKillsOnly).append('\n');
        sb.append("# Keep dropped dog tags and dog tag cases immune to fire and lava\n");
        sb.append("fire_resistant = ").append(fireResistant).append('\n');
        sb.append("# Per-player cooldown in seconds between dog tag drops (0 = unlimited)\n");
        sb.append("cooldown_seconds = ").append(cooldownSeconds).append('\n');
        sb.append("# Enable crafting recipe for the Dog Tag Case\n");
        sb.append("enable_dog_tag_case_recipe = ").append(enableDogTagCaseRecipe).append('\n');
        sb.append("# Enable rare Dog Tag Case loot in structure chests\n");
        sb.append("enable_case_loot = ").append(enableCaseLoot).append('\n');
        sb.append("# Enable Wandering Trader rare trade (20 emeralds -> 1 Dog Tag Case)\n");
        sb.append("enable_wanderer_trade = ").append(enableWandererTrade).append('\n');
        sb.append("# Optional mcfunction id run at death position (permission level 2)\n");
        sb.append("function = ").append(SimpleToml.escapeString(function)).append('\n');
        sb.append("# One entry per lore line, each a JSON text component\n");
        sb.append("lore = [\n");
        for (String line : loreLines) {
            sb.append("  ").append(SimpleToml.formatLiteralOrEscaped(line)).append(",\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String loadDefaultText() throws IOException {
        try (var defaults = KillConfirmedConfig.class.getResourceAsStream("/" + FILE_NAME)) {
            if (defaults == null) {
                return defaults().toToml();
            }
            return new String(defaults.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        if (val == null) return fallback;
        if (val instanceof String s) return s;
        throw new IllegalArgumentException(key + " must be a string");
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean fallback) {
        Object val = map.get(key);
        if (val == null) return fallback;
        if (val instanceof Boolean b) return b;
        throw new IllegalArgumentException(key + " must be a boolean");
    }

    private static int getInt(Map<String, Object> map, String key, int fallback) {
        Object val = map.get(key);
        if (val == null) return fallback;
        if (val instanceof Number n) return n.intValue();
        throw new IllegalArgumentException(key + " must be an integer");
    }

    private static List<String> getStringList(Map<String, Object> map, String key, List<String> fallback) {
        Object val = map.get(key);
        if (val == null) return fallback;
        if (!(val instanceof List<?> raw)) {
            throw new IllegalArgumentException(key + " must be a list of strings");
        }
        if (raw.stream().anyMatch(line -> !(line instanceof String))) {
            throw new IllegalArgumentException("Each " + key + " entry must be a JSON component string");
        }
        return raw.stream().map(String.class::cast).toList();
    }
}
