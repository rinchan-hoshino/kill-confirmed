package dev.rinchan.killconfirmedprobe;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public final class FabricClientProbe implements ClientModInitializer {
    public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(client -> ClientProbe.tick()); }
}
