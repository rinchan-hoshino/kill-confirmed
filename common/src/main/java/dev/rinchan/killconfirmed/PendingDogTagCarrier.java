package dev.rinchan.killconfirmed;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

public interface PendingDogTagCarrier {
    Optional<CompoundTag> killConfirmed$pendingDogTag();
    void killConfirmed$setPendingDogTag(CompoundTag tag);
    Optional<CompoundTag> killConfirmed$takePendingDogTag();
}
