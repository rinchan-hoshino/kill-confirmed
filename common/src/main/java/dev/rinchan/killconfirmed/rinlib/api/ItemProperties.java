package dev.rinchan.killconfirmed.rinlib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Item properties carrying the registry ID on Minecraft versions that require it. */
public final class ItemProperties {
    private ItemProperties() {}

    public static Item.Properties of(String id) {
        ResourceLocation.parse(id);
        return new Item.Properties();
    }
}
