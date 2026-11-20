package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.DogTagLifecycle;
import dev.rinchan.killconfirmed.PendingDogTagHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PendingDogTagHolder {
    @Unique
    private static final String KILL_CONFIRMED_PENDING_KEY = "KillConfirmedPendingDogTag";

    @Unique
    private CompoundTag killConfirmed$pendingDogTag;

    @Override
    public CompoundTag killConfirmed$getPendingDogTag() {
        return this.killConfirmed$pendingDogTag;
    }

    @Override
    public void killConfirmed$setPendingDogTag(CompoundTag tag) {
        this.killConfirmed$pendingDogTag = tag;
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void killConfirmed$onDie(DamageSource source, CallbackInfo ci) {
        DogTagLifecycle.onDeath((ServerPlayer) (Object) this, source);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void killConfirmed$onRestoreFrom(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        DogTagLifecycle.copyPending(oldPlayer, (ServerPlayer) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$savePending(ValueOutput output, CallbackInfo ci) {
        if (this.killConfirmed$pendingDogTag != null) {
            output.store(KILL_CONFIRMED_PENDING_KEY, CompoundTag.CODEC, this.killConfirmed$pendingDogTag.copy());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$loadPending(ValueInput input, CallbackInfo ci) {
        this.killConfirmed$pendingDogTag = input.read(KILL_CONFIRMED_PENDING_KEY, CompoundTag.CODEC)
                .map(CompoundTag::copy)
                .orElse(null);
    }
}
