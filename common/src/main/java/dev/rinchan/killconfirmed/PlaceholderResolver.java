package dev.rinchan.killconfirmed;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.rinchan.killconfirmed.api.PlaceholderContext;
import dev.rinchan.killconfirmed.api.PlaceholderProvider;
import dev.rinchan.killconfirmed.portable.BuiltInFields;
import dev.rinchan.killconfirmed.portable.PlaceholderKey;
import dev.rinchan.killconfirmed.portable.PlaceholderRegistry;
import dev.rinchan.killconfirmed.portable.SnapshotFields;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class PlaceholderResolver {
    private static final PlaceholderRegistry<PlaceholderContext> JAVA_PROVIDERS = new PlaceholderRegistry<>();

    private PlaceholderResolver() {}

    public static void register(ResourceLocation id, PlaceholderProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        PlaceholderKey key = new PlaceholderKey(id.getNamespace(), id.getPath());
        JAVA_PROVIDERS.register(key, context -> provider.resolve(context).map(component -> JsonParser.parseString(
                DogTagSnapshotCodec.json(component, context.player().registryAccess()))));
    }

    static Map<PlaceholderKey, JsonElement> resolve(PlaceholderContext context) {
        var snapshot = context.snapshot();
        SnapshotFields fields = new SnapshotFields(
                json(snapshot.ownerDisplayName(), context),
                snapshot.killer().map(identity -> json(identity.displayName(), context)),
                json(snapshot.deathMessage(), context),
                snapshot.deathPosition().getX(),
                snapshot.deathPosition().getY(),
                snapshot.deathPosition().getZ(),
                snapshot.dimensionId().toString(),
                json(snapshot.dimensionDisplayName(), context),
                snapshot.experienceLevel());

        Map<PlaceholderKey, JsonElement> result = new LinkedHashMap<>(BuiltInFields.from(fields));
        result.putAll(JAVA_PROVIDERS.resolve(context, KillConfirmed.LOGGER::error));
        DatapackPlaceholderBridge.resolve(context.player(), snapshot).forEach((key, value) -> {
            if (result.putIfAbsent(key, value) != null) {
                KillConfirmed.LOGGER.error("Datapack placeholder {} rejected because that key already has an owner", key);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static JsonElement json(net.minecraft.network.chat.Component component, PlaceholderContext context) {
        return JsonParser.parseString(DogTagSnapshotCodec.json(component, context.player().registryAccess()));
    }
}
