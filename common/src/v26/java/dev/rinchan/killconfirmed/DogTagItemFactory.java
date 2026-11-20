package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.PlaceholderContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

final class DogTagItemFactory {
    private DogTagItemFactory() {}

    static ItemStack create(ServerPlayer player, DogTagSnapshot snapshot, DamageSource source) {
        var registries = player.registryAccess();
        ServerLevel serverLevel = player.level();
        CommandSourceStack commandSource = new CommandSourceStack(
                CommandSource.NULL,
                player.position(),
                player.getRotationVector(),
                serverLevel,
                LevelBasedPermissionSet.GAMEMASTER,
                player.getScoreboardName(),
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
        var resolutionContext = ResolutionContext.create(commandSource);
        List<Component> lore = KillConfirmed.config().loreTemplate().render(
                values,
                json -> ComponentUtils.resolve(
                        resolutionContext,
                        DogTagSnapshotCodec.parseComponent(json.toString(), registries)),
                (line, failure) -> KillConfirmed.LOGGER.warn(
                        "Cannot render dog tag lore line {}: {}", line, failure.getMessage()));

        ItemStack stack = new ItemStack(KillConfirmed.dogTag());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "kill_confirmed.tag.name", snapshot.ownerName())
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        CompoundTag custom = new CompoundTag();
        custom.put(DogTagSnapshotCodec.ROOT_KEY, DogTagSnapshotCodec.encode(snapshot, registries));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return stack;
    }
}
