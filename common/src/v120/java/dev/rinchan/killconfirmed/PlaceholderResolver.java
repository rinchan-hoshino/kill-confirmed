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
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class PlaceholderResolver {
    private static final PlaceholderRegistry<PlaceholderContext> JAVA_PROVIDERS = new PlaceholderRegistry<>();

    private PlaceholderResolver() {}

    public static void register(ResourceLocation id, PlaceholderProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        PlaceholderKey key = new PlaceholderKey(id.getNamespace(), id.getPath());
        JAVA_PROVIDERS.register(key, context -> provider.resolve(context)
                .map(component -> JsonParser.parseString(DogTagSnapshotCodec.toJson(component))));
    }

    static Map<PlaceholderKey, JsonElement> resolve(PlaceholderContext context, DamageSource source) {
        var snapshot = context.snapshot();
        SnapshotFields fields = new SnapshotFields(
                json(Component.literal(snapshot.ownerName())),
                snapshot.killer().map(identity -> json(identity.displayName())),
                source.getEntity() instanceof LivingEntity killer && source.getEntity() != context.player()
                        && !killer.getMainHandItem().isEmpty()
                        ? Optional.of(json(killer.getMainHandItem().getHoverName())) : Optional.empty(),
                json(snapshot.deathMessage()),
                json(snapshot.locationDisplayName()),
                snapshot.deathPosition().getX(),
                snapshot.deathPosition().getY(),
                snapshot.deathPosition().getZ(),
                snapshot.dimensionId().toString(),
                json(snapshot.dimensionDisplayName()),
                snapshot.experienceLevel(),
                context.player().level().getDayTime() / 24000L + 1L);

        Map<PlaceholderKey, JsonElement> result = new LinkedHashMap<>(BuiltInFields.from(fields));
        result.putAll(JAVA_PROVIDERS.resolve(context, KillConfirmed.LOGGER::error));
        return Collections.unmodifiableMap(result);
    }

    private static JsonElement json(Component component) {
        return JsonParser.parseString(DogTagSnapshotCodec.toJson(component));
    }
}
