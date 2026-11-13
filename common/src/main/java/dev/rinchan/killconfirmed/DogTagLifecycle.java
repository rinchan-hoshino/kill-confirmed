package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.killconfirmed.rinlib.api.MinecraftData;
import dev.rinchan.killconfirmed.rinlib.api.PlayerContext;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
        if (KillConfirmed.config().playerKillsOnly()
                && (!(source.getEntity() instanceof ServerPlayer) || source.getEntity() == player)) return;
        if (KillConfirmed.PENDING_DOG_TAG.get(player).isPresent()) {
            KillConfirmed.LOGGER.error("Refused to create a second pending dog tag for {}", PlayerContext.name(player));
            return;
        }

        DogTagSnapshot snapshot = capture(player, source);
        ItemStack stack = DogTagItemFactory.create(player, snapshot, source);
        if (KillConfirmed.config().dropStrategy() == DropStrategy.AT_DEATH_POSITION) {
            dropExactlyHere(player, stack, "death");
        } else {
            setPending(player, MinecraftData.of(player.registryAccess()).saveItem(stack));
        }
    }

    public static void copyPending(ServerPlayer original, ServerPlayer replacement) {
        takePending(original)
                .ifPresent(tag -> setPending(replacement, tag));
    }

    public static void onRespawn(ServerPlayer player) {
        Optional<CompoundTag> pending = takePending(player);
        if (pending.isEmpty()) return;

        Optional<ItemStack> decoded = MinecraftData.of(player.registryAccess()).loadItem(pending.get());
        if (decoded.isEmpty() || decoded.get().isEmpty()) {
            KillConfirmed.LOGGER.error("Rejected malformed pending dog tag for {}", PlayerContext.name(player));
            return;
        }

        ItemStack stack = decoded.get();
        player.getInventory().add(stack);
        if (!stack.isEmpty()) dropExactlyHere(player, stack, "respawn");
    }

    private static DogTagSnapshot capture(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getEntity();
        Optional<EntityIdentity> killer = attacker == null || attacker == player
                ? Optional.empty()
                : Optional.of(new EntityIdentity(
                        attacker.getUUID(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()),
                        attacker.getDisplayName()));
        var dimension = player.level().dimension().location();
        String translationKey = "kill_confirmed.dimension." + dimension.getNamespace() + "."
                + dimension.getPath();
        Component dimensionDisplay = Component.translatableWithFallback(translationKey, dimension.toString());
        return new DogTagSnapshot(
                player.getUUID(),
                PlayerContext.name(player),
                player.getDisplayName(),
                killer,
                source.getLocalizedDeathMessage(player),
                player.blockPosition(),
                dimension,
                dimensionDisplay,
                Math.max(0, player.experienceLevel));
    }

    private static void setPending(ServerPlayer player, CompoundTag tag) {
        if (KillConfirmed.PENDING_DOG_TAG.get(player).isPresent())
            throw new IllegalStateException("A dog tag is already pending");
        KillConfirmed.PENDING_DOG_TAG.set(player, tag);
    }

    private static Optional<CompoundTag> takePending(ServerPlayer player) {
        var pending = KillConfirmed.PENDING_DOG_TAG.get(player);
        KillConfirmed.PENDING_DOG_TAG.set(player, null);
        return pending;
    }

    private static void dropExactlyHere(ServerPlayer player, ItemStack stack, String phase) {
        ServerLevel level = (ServerLevel) player.level();
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
        item.setDefaultPickUpDelay();
        if (!level.addFreshEntity(item)) {
            KillConfirmed.LOGGER.error("Failed to create dog tag entity at {} position for {}",
                    phase, PlayerContext.name(player));
        }
    }

}
