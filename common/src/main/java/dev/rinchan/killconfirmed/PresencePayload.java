package dev.rinchan.killconfirmed;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public enum PresencePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<PresencePayload> TYPE = new Type<>(
            KillConfirmed.id("presence_v" + KillConfirmed.NETWORK_VERSION));
    public static final StreamCodec<RegistryFriendlyByteBuf, PresencePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
