package dev.rinchan.killconfirmed;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.killconfirmed.portable.TemplateDocument;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public record KillConfirmedConfig(DropStrategy dropStrategy, TemplateDocument loreTemplate) {
    private static final String DEFAULT_TEMPLATE = "/data/kill_confirmed/lore/default.json";

    public KillConfirmedConfig {
        Objects.requireNonNull(dropStrategy, "dropStrategy");
        Objects.requireNonNull(loreTemplate, "loreTemplate");
    }

    public static KillConfirmedConfig load(Path configDirectory) {
        Path file = configDirectory.resolve("kill_confirmed.json");
        try {
            JsonElement defaultLore = loadDefaultLore();
            if (Files.notExists(file)) {
                Files.createDirectories(configDirectory);
                JsonObject generated = new JsonObject();
                generated.addProperty("drop_strategy", DropStrategy.AT_DEATH_POSITION.name());
                generated.add("lore_template", defaultLore.deepCopy());
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(generated) + "\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }

            JsonElement root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader);
            }
            if (!root.isJsonObject()) throw new IllegalArgumentException("Config root must be an object");
            JsonObject object = root.getAsJsonObject();
            DropStrategy strategy = object.has("drop_strategy")
                    ? DropStrategy.valueOf(object.get("drop_strategy").getAsString())
                    : DropStrategy.AT_DEATH_POSITION;
            JsonElement lore = object.has("lore_template") ? object.get("lore_template") : defaultLore;
            return new KillConfirmedConfig(strategy, TemplateDocument.parse(lore.toString()));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load Kill Confirmed config " + file + ": " + exception.getMessage(), exception);
        }
    }

    private static JsonElement loadDefaultLore() throws IOException {
        try (var stream = KillConfirmedConfig.class.getResourceAsStream(DEFAULT_TEMPLATE)) {
            if (stream == null) throw new IOException("Missing built-in lore template " + DEFAULT_TEMPLATE);
            try (var reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        }
    }
}
