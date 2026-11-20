package dev.rinchan.killconfirmed;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Fire-resistant (configurable) player death dog tag with right-click inspection (1.20.1). */
public final class DogTagItem extends Item {
    public DogTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFireResistant() {
        return KillConfirmed.config().fireResistant();
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
            List<Component> lines = readLoreLines(stack);
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

    public static List<Component> readLoreLines(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains("display", Tag.TAG_COMPOUND)) return List.of();
        CompoundTag display = root.getCompound("display");
        if (!display.contains("Lore", Tag.TAG_LIST)) return List.of();
        ListTag loreTag = display.getList("Lore", Tag.TAG_STRING);
        List<Component> lines = new ArrayList<>(loreTag.size());
        for (int i = 0; i < loreTag.size(); i++) {
            Component parsed = DogTagSnapshotCodec.parseComponent(loreTag.getString(i));
            if (parsed != null) lines.add(parsed);
        }
        return lines;
    }
}
