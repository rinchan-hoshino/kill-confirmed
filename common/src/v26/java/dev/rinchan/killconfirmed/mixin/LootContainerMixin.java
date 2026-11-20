package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainer.class)
public interface LootContainerMixin {
    @Shadow
    @Nullable
    ResourceKey<LootTable> getLootTable();

    @Shadow
    @Nullable
    Level getLevel();

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void killConfirmed$captureLootTable(@Nullable Player player, CallbackInfo ci) {
        if (!KillConfirmed.config().enableCaseLoot()) return;
        ResourceKey<LootTable> tableKey = this.getLootTable();
        if (tableKey == null) return;
        if (!DogTagCaseModel.isEligibleChestLoot(tableKey.identifier().toString())) return;
        Level level = this.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getRandom().nextFloat() >= DogTagCaseModel.STRUCTURE_LOOT_CHANCE) return;
        killConfirmed$scheduleInsert((Container) this, serverLevel);
    }

    @Unique
    private static void killConfirmed$scheduleInsert(Container container, ServerLevel level) {
        level.getServer().execute(() -> {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, new ItemStack(KillConfirmed.dogTagCase()));
                    container.setChanged();
                    return;
                }
            }
        });
    }
}
