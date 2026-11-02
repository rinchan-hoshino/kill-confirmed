package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.api.DogTagSnapshot;
import dev.rinchan.killconfirmed.api.PlaceholderContext;
import dev.rinchan.rinlib.api.MinecraftData;
import dev.rinchan.rinlib.api.PlayerContext;
import dev.rinchan.rinlib.api.FunctionExecution;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class DogTagItemFactory {
    private DogTagItemFactory() {}

    static ItemStack create(ServerPlayer player, DogTagSnapshot snapshot, DamageSource source) {
        var registries = player.registryAccess();
        var commandSource = PlayerContext.functionSource(player).withPosition(player.position());
        String function = KillConfirmed.config().function();
        if (!function.isEmpty()) {
            var server = ((ServerLevel) player.level()).getServer();
            try {
                FunctionExecution.isolated(server, () -> {
                    server.getCommands().performPrefixedCommand(commandSource, "function " + function);
                    return null;
                });
            } catch (RuntimeException failure) {
                KillConfirmed.LOGGER.warn("Dog tag function {} failed", function, failure);
            }
        }
        var values = PlaceholderResolver.resolve(new PlaceholderContext(snapshot, player), source);
        var data = MinecraftData.of(registries);
        java.util.List<Component> lore = KillConfirmed.config().loreTemplate().render(values,
                json -> ComponentUtils.updateForEntity(commandSource, data.component(json.toString()), player, 0),
                (line, failure) -> KillConfirmed.LOGGER.warn("Cannot render dog tag lore line {}: {}", line, failure.getMessage()));
        ItemStack stack = new ItemStack(KillConfirmed.dogTag());
        data.decorate(stack, Component.translatable(
                "kill_confirmed.tag.name", snapshot.ownerName())
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)), lore);

        CompoundTag custom = new CompoundTag();
        custom.put(DogTagSnapshotCodec.ROOT_KEY, DogTagSnapshotCodec.encode(snapshot, registries));
        data.customData(stack, custom);
        return stack;
    }
}
