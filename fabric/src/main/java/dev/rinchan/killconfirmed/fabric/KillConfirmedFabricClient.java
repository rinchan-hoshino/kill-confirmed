package dev.rinchan.killconfirmed.fabric;

import dev.rinchan.killconfirmed.PresencePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class KillConfirmedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PresencePayload.TYPE, (payload, context) -> {});
    }
}
