package dev.rinchan.killconfirmed.rinlib.v2;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import dev.rinchan.killconfirmed.rinlib.v2.net.PresencePayload;
public final class RinNeoForge implements Rin {
    private static boolean eventsRegistered;
    private final String modId;
    private final IEventBus modBus;
    private final DeferredRegister.Items items;
    public RinNeoForge(String modId, IEventBus modBus) {
        this.modId = modId;
        this.modBus = modBus;
        items = DeferredRegister.createItems(modId);
        items.register(modBus);
    }
    public void start(RinMod mod) { registerEvents(); mod.init(this); }
    @Override public String modId() { return modId; }
    @Override public Path configDir() { return FMLPaths.CONFIGDIR.get(); }
    @Override public Supplier<Item> registerItem(String path, Supplier<Item> factory) { return items.register(path, factory); }
    @Override public void requireOnClient(String version) {
        var type = PresencePayload.type(modId, version);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event ->
            event.registrar(version).playToClient(type, PresencePayload.codec(type), (payload, context) -> {}));
    }
    // NeoForge constructs consumer mods in parallel.
    private static synchronized void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer))
                for (var listener : PlayerEvents.DEATH.listeners()) listener.onDeath(player, event.getSource());
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.Clone.class, event -> {
            if (event.isWasDeath() && event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement)
                for (var listener : PlayerEvents.COPY_AFTER_DEATH.listeners()) listener.copy(original, replacement);
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player)
                for (var listener : PlayerEvents.RESPAWN.listeners()) listener.onRespawn(player);
        });
    }
}
