package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.PlaceholderContext;
import dev.rinchan.rinlib.minecraft.MinecraftData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class DogTagItemFactory {
    private DogTagItemFactory() {}

    static ItemStack create(ServerPlayer player, DogTagSnapshot snapshot) {
        var registries = player.registryAccess();
        var values = PlaceholderResolver.resolve(new PlaceholderContext(snapshot, player));
        var rendered = KillConfirmed.config().loreTemplate().render(values);
        var data = MinecraftData.of(registries);
        var lore = rendered.stream().map(data::component).toList();
        ItemStack stack = new ItemStack(KillConfirmed.dogTag());
        data.decorate(stack, Component.translatable(
                "item.kill_confirmed.dog_tag.named", snapshot.ownerDisplayName()).withStyle(ChatFormatting.GOLD), lore);

        CompoundTag custom = new CompoundTag();
        custom.put(DogTagSnapshotCodec.ROOT_KEY, DogTagSnapshotCodec.encode(snapshot, registries));
        data.customData(stack, custom);
        return stack;
    }
}
