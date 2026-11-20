package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.DogTagCaseStorage;
import dev.rinchan.killconfirmed.KillConfirmed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void killConfirmed$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (!KillConfirmed.config().fireResistant()) return;
        ItemStack stack = this.getItem();
        if (DogTagCaseStorage.isDogTag(stack) || DogTagCaseStorage.isDogTagCase(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"),
            cancellable = true)
    private void killConfirmed$autoPickupIntoCase(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (DogTagCaseStorage.tryAutoPickup(serverPlayer, stack)) {
            serverPlayer.take(self, 1);
            if (stack.isEmpty()) {
                self.discard();
                ci.cancel();
            }
        }
    }
}
