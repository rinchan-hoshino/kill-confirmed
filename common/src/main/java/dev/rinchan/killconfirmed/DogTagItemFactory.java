package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.PlaceholderContext;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

final class DogTagItemFactory {
    private DogTagItemFactory() {}

    static ItemStack create(ServerPlayer player, DogTagSnapshot snapshot) {
        var registries = player.registryAccess();
        var values = PlaceholderResolver.resolve(new PlaceholderContext(snapshot, player));
        var rendered = KillConfirmed.config().loreTemplate().render(values);
        if (rendered.size() > ItemLore.MAX_LINES) {
            throw new IllegalArgumentException("Lore template has too many lines: " + rendered.size());
        }

        var lore = new ArrayList<Component>(rendered.size());
        for (var json : rendered) {
            lore.add(DogTagSnapshotCodec.component(json, registries));
        }

        ItemStack stack = new ItemStack(KillConfirmed.dogTag());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.kill_confirmed.dog_tag.named", snapshot.ownerDisplayName()).withStyle(ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        CompoundTag custom = new CompoundTag();
        custom.put(DogTagSnapshotCodec.ROOT_KEY, DogTagSnapshotCodec.encode(snapshot, registries));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return stack;
    }
}
