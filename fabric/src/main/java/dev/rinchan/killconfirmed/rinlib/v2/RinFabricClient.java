package dev.rinchan.killconfirmed.rinlib.v2;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dev.rinchan.killconfirmed.rinlib.v2.net.PresencePayload;
public final class RinFabricClient implements ClientModInitializer {
    private static final List<CustomPacketPayload.Type<PresencePayload>> TYPES = new ArrayList<>();
    public RinFabricClient() {}
    static void require(CustomPacketPayload.Type<PresencePayload> type) { TYPES.add(type); }
    @Override public void onInitializeClient() {
        for (var type : TYPES) ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {});
    }
}
