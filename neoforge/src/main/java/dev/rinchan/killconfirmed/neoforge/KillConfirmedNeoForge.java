package dev.rinchan.killconfirmed.neoforge;

import dev.rinchan.killconfirmed.DogTagCaseMenu;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.VersionFactory;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(KillConfirmed.MOD_ID)
public final class KillConfirmedNeoForge {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KillConfirmed.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KillConfirmed.MOD_ID);

    private static final DeferredHolder<Item, Item> DOG_TAG =
            ITEMS.register("dog_tag", VersionFactory::createDogTag);
    private static final DeferredHolder<Item, Item> DOG_TAG_CASE =
            ITEMS.register("dog_tag_case", VersionFactory::createDogTagCase);
    private static final DeferredHolder<MenuType<?>, MenuType<DogTagCaseMenu>> DOG_TAG_CASE_MENU =
            MENUS.register("dog_tag_case", VersionFactory::createDogTagCaseMenuType);

    public KillConfirmedNeoForge(IEventBus modBus) {
        KillConfirmed.initConfig(FMLPaths.CONFIGDIR.get());
        ITEMS.register(modBus);
        MENUS.register(modBus);
        KillConfirmed.bindRegistries(DOG_TAG::get, DOG_TAG_CASE::get, DOG_TAG_CASE_MENU::get);
        modBus.addListener(this::addCreativeTabItems);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DOG_TAG_CASE.get());
            event.accept(DOG_TAG.get());
        }
    }
}
