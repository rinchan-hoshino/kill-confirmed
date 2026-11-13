package dev.rinchan.killconfirmed.rinlib.v2;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import dev.rinchan.killconfirmed.rinlib.v2.net.PresencePayload;
public final class RinFabric implements Rin {
    private static boolean eventsRegistered;
    private final String modId;
    public RinFabric(String modId) { this.modId = modId; }
    public void start(RinMod mod) {
        registerEvents();
        mod.init(this);
    }
    @Override public String modId() { return modId; }
    @Override public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
    @Override public Supplier<Item> registerItem(String path, Supplier<Item> factory) {
        var item = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(modId, path), factory.get());
        return () -> item;
    }
    @Override public void requireOnClient(String version) {
        var type = PresencePayload.type(modId, version);
        PayloadTypeRegistry.playS2C().register(type, PresencePayload.codec(type));
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) RinFabricClient.require(type);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            if (!ServerPlayNetworking.canSend(player, type)) {
                player.connection.disconnect(Component.translatableWithFallback(
                    "disconnect." + modId + ".required_client", "Kill Confirmed is required on both the client and server."));
                return;
            }
            ServerPlayNetworking.send(player, PresencePayload.of(type));
        });
    }
    private static void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer))
                for (var listener : PlayerEvents.DEATH.listeners()) listener.onDeath(player, source);
        });
        ServerPlayerEvents.COPY_FROM.register((original, replacement, alive) -> {
            if (!alive) for (var listener : PlayerEvents.COPY_AFTER_DEATH.listeners()) listener.copy(original, replacement);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((original, replacement, alive) -> {
            if (!alive) for (var listener : PlayerEvents.RESPAWN.listeners()) listener.onRespawn(replacement);
        });
    }
}
