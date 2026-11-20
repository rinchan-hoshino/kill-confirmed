package dev.rinchan.killconfirmed.mixin;

import dev.rinchan.killconfirmed.KillConfirmed;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
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
    ItemStackTemplate result;

    @Inject(
            method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void killConfirmed$gateCaseRecipe(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!KillConfirmed.config().enableDogTagCaseRecipe()
                && this.result != null
                && this.result.item().value() == KillConfirmed.dogTagCase()) {
            cir.setReturnValue(false);
        }
    }
}
