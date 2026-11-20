package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.KillConfirmed;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
    @Shadow
    @Final
    ItemStack result;

    @Inject(
            method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void killConfirmed$gateCaseRecipe(CraftingContainer input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!KillConfirmed.config().enableDogTagCaseRecipe()
                && this.result != null
                && this.result.getItem() == KillConfirmed.dogTagCase()) {
            cir.setReturnValue(false);
        }
    }
}
