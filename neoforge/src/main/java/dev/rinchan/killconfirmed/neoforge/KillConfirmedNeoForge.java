package dev.rinchan.killconfirmed.neoforge;

import dev.rinchan.killconfirmed.DogTagLifecycle;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.PresencePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(KillConfirmed.MOD_ID)
public final class KillConfirmedNeoForge {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KillConfirmed.MOD_ID);
    private static final DeferredItem<Item> DOG_TAG = ITEMS.register("dog_tag",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public KillConfirmedNeoForge(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::registerPayloads);
        KillConfirmed.initialize(DOG_TAG, FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class,
                KillConfirmedNeoForge::onDeath);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.Clone.class, KillConfirmedNeoForge::onClone);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, KillConfirmedNeoForge::onRespawn);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(KillConfirmed.NETWORK_VERSION).playToClient(
                PresencePayload.TYPE, PresencePayload.CODEC, (payload, context) -> {});
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            DogTagLifecycle.onDeath(player, event.getSource());
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement) {
            DogTagLifecycle.copyPending(original, replacement);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) DogTagLifecycle.onRespawn(player);
    }
}
