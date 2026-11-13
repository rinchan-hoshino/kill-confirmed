package dev.rinchan.killconfirmed.rinlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Registry-aware text and item serialization across Minecraft versions. */
public final class MinecraftData {
    private final HolderLookup.Provider registries;

    private MinecraftData(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    public static MinecraftData of(HolderLookup.Provider registries) {
        return new MinecraftData(registries);
    }

    public Component component(JsonElement json) {
        return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json)
                .getOrThrow();
    }

    public Component component(String json) {
        return component(JsonParser.parseString(json));
    }

    public String json(Component component) {
        return ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
                .getOrThrow().toString();
    }

    public CompoundTag saveItem(ItemStack stack) {
        return (CompoundTag) ItemStack.CODEC.encodeStart(
                RegistryOps.create(NbtOps.INSTANCE, registries), stack).getOrThrow();
    }

    public Optional<ItemStack> loadItem(CompoundTag tag) {
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag).result();
    }

    public CompoundTag customData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public void customData(ItemStack stack, CompoundTag data) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public void decorate(ItemStack stack, Component name, List<Component> lore) {
        if (lore.size() > ItemLore.MAX_LINES) {
            throw new IllegalArgumentException("Item lore has too many lines: " + lore.size());
        }
        stack.set(DataComponents.CUSTOM_NAME, name);
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }
}
