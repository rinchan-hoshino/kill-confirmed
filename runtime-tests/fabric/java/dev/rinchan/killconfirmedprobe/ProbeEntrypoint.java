package dev.rinchan.killconfirmedprobe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
public final class ProbeEntrypoint implements ModInitializer {
    private int ticks;
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.overworld().setChunkForced(0, 0, true));
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (++ticks == 20) RuntimeProbe.run(server); });
    }
}
