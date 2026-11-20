package dev.rinchan.killconfirmed.fabric.client;

import dev.rinchan.killconfirmed.client.KillConfirmedClientHooks;
import net.fabricmc.api.ClientModInitializer;

public final class KillConfirmedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KillConfirmedClientHooks.init();
    }
}
