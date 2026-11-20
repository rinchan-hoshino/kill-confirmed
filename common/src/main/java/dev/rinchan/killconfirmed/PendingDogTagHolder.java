package dev.rinchan.killconfirmed;

import net.minecraft.nbt.CompoundTag;

/** Duck interface injected onto ServerPlayer to persist a pending respawn dog tag without RinLib. */
public interface PendingDogTagHolder {
    CompoundTag killConfirmed$getPendingDogTag();

    void killConfirmed$setPendingDogTag(CompoundTag tag);
}
