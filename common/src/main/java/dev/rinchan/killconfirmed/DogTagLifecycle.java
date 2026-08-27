package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.EntityIdentity;
import dev.rinchan.killconfirmed.portable.DropAction;
import dev.rinchan.killconfirmed.portable.DropStrategyMachine;
import dev.rinchan.killconfirmed.portable.PendingState;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class DogTagLifecycle {
    private DogTagLifecycle() {}

    public static void onDeath(ServerPlayer player, DamageSource source) {
        PendingDogTagCarrier carrier = carrier(player);
        PendingState pending = carrier.killConfirmed$pendingDogTag().isPresent()
                ? PendingState.PENDING : PendingState.EMPTY;
        var transition = DropStrategyMachine.onDeath(KillConfirmed.config().dropStrategy(), pending);
        if (transition.action() == DropAction.NOOP) {
            KillConfirmed.LOGGER.error("Refused to create a second pending dog tag for {}", player.getGameProfile().name());
            return;
        }

        DogTagSnapshot snapshot = capture(player, source);
        ItemStack stack = DogTagItemFactory.create(player, snapshot);
        if (transition.action() == DropAction.DROP_AT_DEATH) {
            dropExactlyHere(player, stack, "death");
        } else if (transition.action() == DropAction.STORE_PENDING) {
            TagAssertions.compound(ItemStack.CODEC.encodeStart(
                    player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow(),
                    carrier::killConfirmed$setPendingDogTag);
        } else {
            throw new IllegalStateException("Unexpected death transition " + transition.action());
        }
    }

    public static void copyPending(ServerPlayer original, ServerPlayer replacement) {
        carrier(original).killConfirmed$takePendingDogTag()
                .ifPresent(tag -> carrier(replacement).killConfirmed$setPendingDogTag(tag));
    }

    public static void onRespawn(ServerPlayer player) {
        Optional<CompoundTag> pending = carrier(player).killConfirmed$takePendingDogTag();
        if (pending.isEmpty()) return;

        Optional<ItemStack> decoded = ItemStack.CODEC.parse(
                player.registryAccess().createSerializationContext(NbtOps.INSTANCE), pending.get()).result();
        if (decoded.isEmpty() || decoded.get().isEmpty()) {
            KillConfirmed.LOGGER.error("Rejected malformed pending dog tag for {}", player.getGameProfile().name());
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
        Identifier dimension = player.level().dimension().identifier();
        String translationKey = "dimension." + dimension.getNamespace() + "."
                + dimension.getPath().replace('/', '.');
        Component dimensionDisplay = Component.translatableWithFallback(translationKey, dimension.toString());
        return new DogTagSnapshot(
                player.getUUID(),
                player.getGameProfile().name(),
                player.getDisplayName(),
                killer,
                source.getLocalizedDeathMessage(player),
                player.blockPosition(),
                dimension,
                dimensionDisplay,
                Math.max(0, player.experienceLevel));
    }

    private static PendingDogTagCarrier carrier(ServerPlayer player) {
        return (PendingDogTagCarrier) player;
    }

    private static void dropExactlyHere(ServerPlayer player, ItemStack stack, String phase) {
        ServerLevel level = player.level();
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
        item.setDefaultPickUpDelay();
        if (!level.addFreshEntity(item)) {
            KillConfirmed.LOGGER.error("Failed to create dog tag entity at {} position for {}",
                    phase, player.getGameProfile().name());
        }
    }

    private static final class TagAssertions {
        private TagAssertions() {}

        static void compound(net.minecraft.nbt.Tag tag, java.util.function.Consumer<CompoundTag> consumer) {
            if (!(tag instanceof CompoundTag compound)) {
                throw new IllegalStateException("Serialized dog tag was not a compound");
            }
            consumer.accept(compound);
        }
    }
}
