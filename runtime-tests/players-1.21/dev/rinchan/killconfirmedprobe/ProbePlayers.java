package dev.rinchan.killconfirmedprobe;
import dev.rinchan.killconfirmed.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public final class ProbePlayers {
    public static ServerPlayer create(MinecraftServer server, String name, long id) {
        var profile = new GameProfile(new UUID(0L, id), name);
        var player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        ProbeNetwork.configure(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false));
        return player;
    }
    public static CompoundTag save(ServerPlayer player) { return player.saveWithoutId(new CompoundTag()); }
    public static void load(ServerPlayer player, CompoundTag tag) { player.load(tag); }
    public static void kill(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) { player.hurt(source, 1000); }

}
