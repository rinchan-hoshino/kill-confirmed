package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class LootContainerMixin {
    @Shadow
    @Nullable
    protected ResourceLocation lootTable;

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void killConfirmed$captureLootTable(@Nullable Player player, CallbackInfo ci) {
        if (!KillConfirmed.config().enableCaseLoot()) return;
        ResourceLocation tableId = this.lootTable;
        if (tableId == null) return;
        if (!DogTagCaseModel.isEligibleChestLoot(tableId.toString())) return;
        RandomizableContainerBlockEntity self = (RandomizableContainerBlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getRandom().nextFloat() >= DogTagCaseModel.STRUCTURE_LOOT_CHANCE) return;
        Container container = self;
        serverLevel.getServer().execute(() -> {
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
