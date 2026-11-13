package dev.rinchan.killconfirmed.rinlib.v2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
public final class PlayerEvents {
    public static final Event<Death> DEATH = new Event<>();
    public static final Event<CopyAfterDeath> COPY_AFTER_DEATH = new Event<>();
    public static final Event<Respawn> RESPAWN = new Event<>();
    private PlayerEvents() {}
    @FunctionalInterface public interface Death { void onDeath(ServerPlayer p, DamageSource src); }
    @FunctionalInterface public interface CopyAfterDeath { void copy(ServerPlayer original, ServerPlayer replacement); }
    @FunctionalInterface public interface Respawn { void onRespawn(ServerPlayer p); }
}
