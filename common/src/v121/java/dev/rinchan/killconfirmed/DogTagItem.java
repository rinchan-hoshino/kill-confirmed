package dev.rinchan.killconfirmed;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Fire-resistant (configurable) player death dog tag with right-click inspection. */
public final class DogTagItem extends Item {
    public DogTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.CHAIN_STEP,
                SoundSource.PLAYERS,
                0.35f,
                1.55f);

        if (!level.isClientSide) {
            ItemLore lore = stack.get(DataComponents.LORE);
            List<Component> lines = lore != null ? lore.lines() : List.of();
            if (lines.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("kill_confirmed.inspect.empty").withStyle(ChatFormatting.GRAY),
                        true);
            } else {
                player.displayClientMessage(
                        Component.literal("— ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(" —").withStyle(ChatFormatting.DARK_GRAY)),
                        false);
                for (Component line : lines) {
                    player.displayClientMessage(line, false);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
