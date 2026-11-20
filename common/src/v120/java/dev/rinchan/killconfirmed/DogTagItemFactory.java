package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.PlaceholderContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

final class DogTagItemFactory {
    private DogTagItemFactory() {}

    static ItemStack create(ServerPlayer player, DogTagSnapshot snapshot, DamageSource source) {
        ServerLevel serverLevel = player.serverLevel();
        CommandSourceStack commandSource = new CommandSourceStack(
                CommandSource.NULL,
                player.position(),
                player.getRotationVector(),
                serverLevel,
                2,
                player.getGameProfile().getName(),
                player.getDisplayName(),
                serverLevel.getServer(),
                player).withSuppressedOutput();

        String function = KillConfirmed.config().function();
        if (!function.isEmpty()) {
            try {
                serverLevel.getServer().getCommands().performPrefixedCommand(commandSource, "function " + function);
            } catch (RuntimeException failure) {
                KillConfirmed.LOGGER.warn("Dog tag function {} failed", function, failure);
            }
        }

        var values = PlaceholderResolver.resolve(new PlaceholderContext(snapshot, player), source);
        List<Component> lore = KillConfirmed.config().loreTemplate().render(
                values,
                json -> ComponentUtils.updateForEntity(
                        commandSource,
                        DogTagSnapshotCodec.parseComponent(json.toString()),
                        player,
                        0),
                (line, failure) -> KillConfirmed.LOGGER.warn(
                        "Cannot render dog tag lore line {}: {}", line, failure.getMessage()));

        ItemStack stack = new ItemStack(KillConfirmed.dogTag());
        stack.setHoverName(Component.translatable("kill_confirmed.tag.name", snapshot.ownerName())
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));

        CompoundTag root = stack.getOrCreateTag();
        CompoundTag display = root.getCompound("display");
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(DogTagSnapshotCodec.toJson(line)));
        }
        display.put("Lore", loreTag);
        root.put("display", display);
        root.put(DogTagSnapshotCodec.ROOT_KEY, DogTagSnapshotCodec.encode(snapshot));
        return stack;
    }
}
