package dev.rinchan.killconfirmed.rinlib.v2.net;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public final class PresencePayload implements CustomPacketPayload {
    private final Type<PresencePayload> type;
    private PresencePayload(Type<PresencePayload> type) { this.type = type; }
    public static Type<PresencePayload> type(String modId, String version) {
        return new Type<>(ResourceLocation.fromNamespaceAndPath(modId, "presence_v" + version));
    }
    public static StreamCodec<RegistryFriendlyByteBuf, PresencePayload> codec(Type<PresencePayload> type) {
        return StreamCodec.unit(new PresencePayload(type));
    }
    public static PresencePayload of(Type<PresencePayload> type) { return new PresencePayload(type); }
    @Override public Type<PresencePayload> type() { return type; }
    @Override public boolean equals(Object other) {
        return other instanceof PresencePayload payload && type().id().equals(payload.type().id());
    }
    @Override public int hashCode() { return type().id().hashCode(); }
}
