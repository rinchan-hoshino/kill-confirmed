package dev.rinchan.killconfirmed;

import java.lang.reflect.Proxy;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Version-layer factory for creating items, menu types, and Fabric direct registration (26.x). */
public final class VersionFactory {
    private VersionFactory() {}

    public static Item createDogTag() {
        return new DogTagItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KillConfirmed.MOD_ID, "dog_tag")))
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    public static Item createDogTagCase() {
        return new DogTagCaseItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KillConfirmed.MOD_ID, "dog_tag_case")))
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @SuppressWarnings("unchecked")
    public static MenuType<DogTagCaseMenu> createDogTagCaseMenuType() {
        try {
            var ctor = MenuType.class.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Class<?> supplierIface = ctor.getParameterTypes()[0];
            Object supplier = Proxy.newProxyInstance(
                    supplierIface.getClassLoader(),
                    new Class<?>[]{supplierIface},
                    (proxy, method, args) -> new DogTagCaseMenu((Integer) args[0], (Inventory) args[1]));
            return (MenuType<DogTagCaseMenu>) ctor.newInstance(supplier, FeatureFlags.VANILLA_SET);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create DogTagCase MenuType", e);
        }
    }

    public static void registerFabric() {
        Item dogTag = Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(KillConfirmed.MOD_ID, "dog_tag"),
                createDogTag());
        Item dogTagCase = Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(KillConfirmed.MOD_ID, "dog_tag_case"),
                createDogTagCase());
        MenuType<DogTagCaseMenu> menuType = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(KillConfirmed.MOD_ID, "dog_tag_case"),
                createDogTagCaseMenuType());
        KillConfirmed.bindRegistries(() -> dogTag, () -> dogTagCase, () -> menuType);
    }
}
