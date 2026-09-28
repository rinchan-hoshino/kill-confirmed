package dev.rinchan.killconfirmedprobe;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@Mod("kill_confirmed_probe")
public final class ProbeEntrypoint {
    private int ticks;
    public ProbeEntrypoint() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> event.getServer().overworld().setChunkForced(0, 0, true));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> { if (++ticks == 20) RuntimeProbe.run(event.getServer()); });
    }
}
