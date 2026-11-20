package dev.rinchan.killconfirmed.fabric.client.mixin;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.client.DogTagCaseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuScreens.class)
public abstract class MenuScreensMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static <T extends AbstractContainerMenu> void killConfirmed$openCaseScreen(
            MenuType<T> type,
            Minecraft minecraft,
            int containerId,
            Component title,
            CallbackInfo ci) {
        if (type == KillConfirmed.dogTagCaseMenuType()) {
            DogTagCaseScreen.open(minecraft, containerId, title);
            ci.cancel();
        }
    }
}
