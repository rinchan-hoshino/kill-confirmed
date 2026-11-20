package dev.rinchan.killconfirmed.neoforge.client;

import dev.rinchan.killconfirmed.DogTagCaseMenu;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.client.DogTagCaseScreen;
import dev.rinchan.killconfirmed.client.KillConfirmedClientHooks;
import dev.rinchan.killconfirmed.client.KillConfirmedConfigScreen;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = KillConfirmed.MOD_ID, dist = Dist.CLIENT)
public final class KillConfirmedNeoForgeClient {
    public KillConfirmedNeoForgeClient(ModContainer container, IEventBus modBus) {
        KillConfirmedClientHooks.init();
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new KillConfirmedConfigScreen(parent));
        modBus.addListener(this::registerMenuScreens);
    }

    @SuppressWarnings("unchecked")
    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(
                (MenuType<DogTagCaseMenu>) KillConfirmed.dogTagCaseMenuType(),
                DogTagCaseScreen::new);
    }
}
