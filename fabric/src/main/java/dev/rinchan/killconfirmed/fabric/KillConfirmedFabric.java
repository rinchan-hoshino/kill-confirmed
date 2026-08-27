package dev.rinchan.killconfirmed.fabric;

import dev.rinchan.killconfirmed.DogTagLifecycle;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.PresencePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class KillConfirmedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Item dogTag = Registry.register(BuiltInRegistries.ITEM, KillConfirmed.DOG_TAG_ID,
                new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        KillConfirmed.initialize(() -> dogTag, FabricLoader.getInstance().getConfigDir());
        PayloadTypeRegistry.playS2C().register(PresencePayload.TYPE, PresencePayload.CODEC);

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
                DogTagLifecycle.onDeath(player, source);
            }
        });
        ServerPlayerEvents.COPY_FROM.register((original, replacement, alive) -> {
            if (!alive) DogTagLifecycle.copyPending(original, replacement);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((original, replacement, alive) -> {
            if (!alive) DogTagLifecycle.onRespawn(replacement);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!ServerPlayNetworking.canSend(player, PresencePayload.TYPE)) {
                player.connection.disconnect(Component.translatableWithFallback(
                        "disconnect.kill_confirmed.required_client",
                        "Kill Confirmed is required on both the client and server."));
                return;
            }
            ServerPlayNetworking.send(player, PresencePayload.INSTANCE);
        });
    }
}
