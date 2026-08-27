package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.PendingDogTagCarrier;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private void killConfirmed$savePending(ValueOutput output, CallbackInfo callback) {
        if (killConfirmed$pending != null) {
            output.store(KILL_CONFIRMED_PENDING, CompoundTag.CODEC, killConfirmed$pending.copy());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$loadPending(ValueInput input, CallbackInfo callback) {
        killConfirmed$pending = input.read(KILL_CONFIRMED_PENDING, CompoundTag.CODEC)
                .map(CompoundTag::copy)
                .orElse(null);
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
