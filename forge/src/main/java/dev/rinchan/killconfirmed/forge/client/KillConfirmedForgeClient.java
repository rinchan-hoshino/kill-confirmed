package dev.rinchan.killconfirmed.forge.client;

import dev.rinchan.killconfirmed.DogTagCaseMenu;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.client.DogTagCaseScreen;
import dev.rinchan.killconfirmed.client.KillConfirmedClientHooks;
import dev.rinchan.killconfirmed.client.KillConfirmedConfigScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class KillConfirmedForgeClient {
    private KillConfirmedForgeClient() {}

    public static void init(IEventBus modBus) {
        KillConfirmedClientHooks.init();
        modBus.addListener(KillConfirmedForgeClient::onClientSetup);
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new KillConfirmedConfigScreen(parent)));
    }

    @SuppressWarnings("unchecked")
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
                (MenuType<DogTagCaseMenu>) KillConfirmed.dogTagCaseMenuType(),
                DogTagCaseScreen::new));
    }
}
