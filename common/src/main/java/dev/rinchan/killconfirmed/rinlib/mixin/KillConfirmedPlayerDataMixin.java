package dev.rinchan.killconfirmed.rinlib.mixin;

import dev.rinchan.killconfirmed.rinlib.v2.PlayerData;
import dev.rinchan.killconfirmed.rinlib.v2.internal.PlayerDataAccess;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class KillConfirmedPlayerDataMixin implements PlayerDataAccess {
    @Unique private Map<String, CompoundTag> killConfirmed$data = new HashMap<>();
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$save(CompoundTag root, CallbackInfo callback) { for (var key : PlayerData.keys()) { var value = killConfirmed$data.get(key); if (value != null) root.put(key, value.copy()); } }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void killConfirmed$load(CompoundTag root, CallbackInfo callback) { for (var key : PlayerData.keys()) { if (root.contains(key, Tag.TAG_COMPOUND)) killConfirmed$data.put(key, root.getCompound(key).copy()); else killConfirmed$data.remove(key); } }
    @Override public Map<String, CompoundTag> killConfirmed$data() { return killConfirmed$data; }
}
