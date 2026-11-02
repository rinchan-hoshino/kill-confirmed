package dev.rinchan.killconfirmed;

import com.electronwill.nightconfig.toml.TomlParser;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.killconfirmed.portable.TemplateDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public record KillConfirmedConfig(DropStrategy dropStrategy, boolean playerKillsOnly,
        String function, TemplateDocument loreTemplate) {
    public static KillConfirmedConfig load(Path configDirectory) {
        Path file = configDirectory.resolve("kill_confirmed.toml");
        try (var defaults = KillConfirmedConfig.class.getResourceAsStream("/kill_confirmed.toml")) {
            if (defaults == null) throw new IOException("Missing built-in kill_confirmed.toml");
            String defaultText = new String(defaults.readAllBytes(), StandardCharsets.UTF_8);
            if (Files.notExists(file)) {
                Files.createDirectories(configDirectory);
                Files.writeString(file, defaultText, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            var parser = new TomlParser();
            var builtIn = parser.parse(defaultText);
            var config = parser.parse(Files.readString(file, StandardCharsets.UTF_8));
            String drop = config.getOrElse("drop", (String) builtIn.get("drop"));
            DropStrategy strategy = switch (drop) {
                case "death_position" -> DropStrategy.AT_DEATH_POSITION;
                case "respawn_inventory" -> DropStrategy.ON_RESPAWN_INVENTORY;
                default -> throw new IllegalArgumentException("Unknown drop strategy: " + drop);
            };
            boolean playerKillsOnly = config.getOrElse("player_kills_only", false);
            String function = config.getOrElse("function", "");
            if (!function.isEmpty() && !function.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
                throw new IllegalArgumentException("Invalid function id: " + function);
            List<?> lines = config.getOrElse("lore", (List<?>) builtIn.get("lore"));
            if (lines.stream().anyMatch(line -> !(line instanceof String)))
                throw new IllegalArgumentException("Each lore entry must be a JSON component string");
            return new KillConfirmedConfig(strategy, playerKillsOnly, function,
                    new TemplateDocument(lines.stream().map(String.class::cast).toList()));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load Kill Confirmed config " + file + ": " + exception.getMessage(), exception);
        }
    }
}
