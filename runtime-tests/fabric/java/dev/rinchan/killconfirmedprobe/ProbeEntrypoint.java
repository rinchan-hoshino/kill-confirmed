package dev.rinchan.killconfirmedprobe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
public final class ProbeEntrypoint implements ModInitializer {
    public static boolean bundledLibrary() {
        var origin = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("rinlib").orElseThrow().getOrigin();
        return origin.getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED
                && origin.getParentModId().equals("kill_confirmed");
    }
    private int ticks;
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.overworld().setChunkForced(0, 0, true));
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (++ticks == 20) RuntimeProbe.run(server); });
    }
}
