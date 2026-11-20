package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** 108-slot fire-resistant vanilla Dog Tag Case with Bundle-style quick insert/extract (26.x). */
public final class DogTagCaseItem extends Item {
    public DogTagCaseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int lockedSlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().getSelectedSlot()
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new DogTagCaseMenu(containerId, inventory, stack, lockedSlot),
                    stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack caseStack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || caseStack.getCount() != 1) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            ItemStack popped = DogTagCaseStorage.popLast(caseStack, player.registryAccess());
            if (!popped.isEmpty()) {
                slot.safeInsert(popped);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
                return true;
            }
            return false;
        }
        if (DogTagCaseStorage.isDogTag(target) && slot.mayPickup(player)) {
            if (DogTagCaseStorage.insertSingle(caseStack, target, player.registryAccess())) {
                slot.safeTake(1, 1, player);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack caseStack, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || caseStack.getCount() != 1) return false;
        if (carried.isEmpty()) {
            ItemStack popped = DogTagCaseStorage.popLast(caseStack, player.registryAccess());
            if (!popped.isEmpty()) {
                access.set(popped);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
                return true;
            }
            return false;
        }
        if (DogTagCaseStorage.isDogTag(carried)) {
            if (DogTagCaseStorage.insertSingle(caseStack, carried, player.registryAccess())) {
                carried.shrink(1);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        int count = DogTagCaseStorage.count(stack);
        boolean autoPickup = DogTagCaseStorage.isAutoPickup(stack);
        adder.accept(Component.translatable("kill_confirmed.case.stored", count, DogTagCaseModel.CAPACITY)
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable(
                        autoPickup ? "kill_confirmed.case.auto_pickup.on" : "kill_confirmed.case.auto_pickup.off")
                .withStyle(autoPickup ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));

        if (count > 0) {
            if (KillConfirmed.isShiftDown()) {
                var top = DogTagCaseModel.topVictims(DogTagCaseStorage.readOwnerNames(stack), 3);
                for (var entry : top) {
                    adder.accept(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable("kill_confirmed.case.stats.entry", entry.name(), entry.count())
                                    .withStyle(ChatFormatting.GOLD)));
                }
            } else {
                adder.accept(Component.translatable("kill_confirmed.case.shift_hint")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }
}
