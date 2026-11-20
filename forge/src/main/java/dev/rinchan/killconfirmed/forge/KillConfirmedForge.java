package dev.rinchan.killconfirmed.forge;

import dev.rinchan.killconfirmed.DogTagCaseMenu;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.VersionFactory;
import dev.rinchan.killconfirmed.client.DogTagCaseScreen;
import dev.rinchan.killconfirmed.client.KillConfirmedConfigScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(KillConfirmed.MOD_ID)
public final class KillConfirmedForge {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KillConfirmed.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, KillConfirmed.MOD_ID);

    private static final RegistryObject<Item> DOG_TAG =
            ITEMS.register("dog_tag", VersionFactory::createDogTag);
    private static final RegistryObject<Item> DOG_TAG_CASE =
            ITEMS.register("dog_tag_case", VersionFactory::createDogTagCase);
    private static final RegistryObject<MenuType<DogTagCaseMenu>> DOG_TAG_CASE_MENU =
            MENUS.register("dog_tag_case", VersionFactory::createDogTagCaseMenuType);

    public KillConfirmedForge() {
        KillConfirmed.initConfig(FMLPaths.CONFIGDIR.get());
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        MENUS.register(modBus);
        KillConfirmed.bindRegistries(DOG_TAG::get, DOG_TAG_CASE::get, DOG_TAG_CASE_MENU::get);
        modBus.addListener(this::addCreativeTabItems);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::onClientSetup);
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new KillConfirmedConfigScreen(parent)));
        });
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DOG_TAG_CASE.get());
            event.accept(DOG_TAG.get());
        }
    }

    @SuppressWarnings("unchecked")
    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
                (MenuType<DogTagCaseMenu>) KillConfirmed.dogTagCaseMenuType(),
                DogTagCaseScreen::new));
    }
}
