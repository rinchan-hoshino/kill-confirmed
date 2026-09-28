package dev.rinchan.killconfirmedprobe;
import dev.rinchan.killconfirmed.*;
import net.minecraft.network.Connection;
public final class ProbeNetwork {
    public static void configure(Connection connection) { new io.netty.channel.embedded.EmbeddedChannel(connection); }
}
