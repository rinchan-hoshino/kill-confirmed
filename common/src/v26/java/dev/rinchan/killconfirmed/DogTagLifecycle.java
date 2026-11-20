package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import dev.rinchan.killconfirmed.portable.DogTagRule;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class DogTagLifecycle {
    private DogTagLifecycle() {}

    public static void onDeath(ServerPlayer player, DamageSource source) {
        KillConfirmedConfig cfg = KillConfirmed.config();
        boolean killedByOtherPlayer = source.getEntity() instanceof ServerPlayer killer && killer != player;
        if (!DogTagRule.createsTag(cfg.playerKillsOnly(), killedByOtherPlayer)) return;

        if (!KillConfirmed.cooldownTracker().allowAndRecord(
                player.getUUID(), System.currentTimeMillis(), cfg.cooldownSeconds())) {
            return;
        }

        PendingDogTagHolder holder = (PendingDogTagHolder) player;
        if (holder.killConfirmed$getPendingDogTag() != null) {
            KillConfirmed.LOGGER.warn("Skipped duplicate pending dog tag for {}", player.getScoreboardName());
            return;
        }

        DogTagSnapshot snapshot = capture(player, source);
        ItemStack stack = DogTagItemFactory.create(player, snapshot, source);
        if (cfg.dropStrategy() == DropStrategy.AT_DEATH_POSITION) {
            dropExactlyHere(player, stack, "death");
        } else {
            var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(tag -> {
                if (tag instanceof CompoundTag compound) {
                    holder.killConfirmed$setPendingDogTag(compound);
                }
            });
        }
    }

    public static void copyPending(ServerPlayer original, ServerPlayer replacement) {
        PendingDogTagHolder from = (PendingDogTagHolder) original;
        PendingDogTagHolder to = (PendingDogTagHolder) replacement;
        CompoundTag pending = from.killConfirmed$getPendingDogTag();
        if (pending != null) {
            from.killConfirmed$setPendingDogTag(null);
            to.killConfirmed$setPendingDogTag(pending.copy());
        }
    }

    public static void onRespawn(ServerPlayer player) {
        PendingDogTagHolder holder = (PendingDogTagHolder) player;
        CompoundTag pending = holder.killConfirmed$getPendingDogTag();
        if (pending == null) return;
        holder.killConfirmed$setPendingDogTag(null);

        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Optional<ItemStack> decoded = ItemStack.CODEC.parse(ops, pending).result();
        if (decoded.isEmpty() || decoded.get().isEmpty()) {
            KillConfirmed.LOGGER.error("Rejected malformed pending dog tag for {}", player.getScoreboardName());
            return;
        }

        ItemStack stack = decoded.get();
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            dropExactlyHere(player, stack, "respawn");
        }
    }

    private static DogTagSnapshot capture(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getEntity();
        Optional<EntityIdentity> killer = attacker == null || attacker == player
                ? Optional.empty()
                : Optional.of(new EntityIdentity(
                        attacker.getUUID(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()),
                        attacker.getDisplayName()));
        ServerLevel level = player.level();
        var dimension = level.dimension().identifier();
        String translationKey = "kill_confirmed.dimension." + dimension.getNamespace() + "." + dimension.getPath();
        Component dimensionDisplay = Component.translatableWithFallback(translationKey, dimension.toString());
        DogTagLandmarks.Landmark landmark = DogTagLandmarks.resolve(level, player.blockPosition());
        return new DogTagSnapshot(
                player.getUUID(),
                player.getScoreboardName(),
                player.getDisplayName(),
                killer,
                source.getLocalizedDeathMessage(player),
                player.blockPosition(),
                dimension,
                dimensionDisplay,
                landmark.id(),
                landmark.displayName(),
                Math.max(0, player.experienceLevel));
    }

    private static void dropExactlyHere(ServerPlayer player, ItemStack stack, String phase) {
        ServerLevel level = player.level();
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
        item.setDefaultPickUpDelay();
        if (!level.addFreshEntity(item)) {
            KillConfirmed.LOGGER.error("Failed to drop dog tag at {} for {}",
                    phase, player.getScoreboardName());
        }
    }
}
