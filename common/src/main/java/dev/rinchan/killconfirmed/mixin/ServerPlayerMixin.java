package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.PendingDogTagCarrier;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PendingDogTagCarrier {
    @Unique private static final String KILL_CONFIRMED_PENDING = "KillConfirmedPendingDogTag";
    @Unique private CompoundTag killConfirmed$pending;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$savePending(CompoundTag root, CallbackInfo callback) {
        if (killConfirmed$pending != null) root.put(KILL_CONFIRMED_PENDING, killConfirmed$pending.copy());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$loadPending(CompoundTag root, CallbackInfo callback) {
        killConfirmed$pending = root.contains(KILL_CONFIRMED_PENDING, Tag.TAG_COMPOUND)
                ? root.getCompound(KILL_CONFIRMED_PENDING).copy()
                : null;
    }

    @Override
    public Optional<CompoundTag> killConfirmed$pendingDogTag() {
        return Optional.ofNullable(killConfirmed$pending).map(CompoundTag::copy);
    }

    @Override
    public void killConfirmed$setPendingDogTag(CompoundTag tag) {
        if (killConfirmed$pending != null) throw new IllegalStateException("A dog tag is already pending");
        killConfirmed$pending = tag.copy();
    }

    @Override
    public Optional<CompoundTag> killConfirmed$takePendingDogTag() {
        CompoundTag result = killConfirmed$pending;
        killConfirmed$pending = null;
        return Optional.ofNullable(result).map(CompoundTag::copy);
    }
}
