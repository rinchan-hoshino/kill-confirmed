package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.SortMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Pure vanilla dual-section container menu for the 108-slot Dog Tag Case (zero custom packets). */
public final class DogTagCaseMenu extends AbstractContainerMenu {
    public static final int BUTTON_SCROLL_BASE = 0; // 0..6
    public static final int BUTTON_TOGGLE_AUTO_PICKUP = 10;
    public static final int BUTTON_CYCLE_SORT = 11;

    private static final int DATA_SCROLL_ROW = 0;
    private static final int DATA_AUTO_PICKUP = 1;
    private static final int DATA_SORT_MODE = 2;
    private static final int DATA_FILLED_COUNT = 3;
    private static final int DATA_LOCKED_SLOT = 4;

    private final Inventory playerInventory;
    private final ItemStack caseStack;
    private final boolean serverSide;
    private final SimpleContainer backingContainer;
    private final SimpleContainer clientWindow;
    private final long[] entryOrders = new long[DogTagCaseModel.CAPACITY];
    private final ContainerData data;
    private int scrollRow = 0;

    /** Client constructor. */
    @SuppressWarnings("unchecked")
    public DogTagCaseMenu(int containerId, Inventory playerInventory) {
        super((MenuType<DogTagCaseMenu>) KillConfirmed.dogTagCaseMenuType(), containerId);
        this.playerInventory = playerInventory;
        this.caseStack = ItemStack.EMPTY;
        this.serverSide = false;
        this.backingContainer = new SimpleContainer(DogTagCaseModel.CAPACITY);
        this.clientWindow = new SimpleContainer(DogTagCaseModel.VISIBLE_SLOTS);
        this.data = new SimpleContainerData(5);
        this.addDataSlots(this.data);
        buildSlots();
    }

    /** Server constructor. */
    @SuppressWarnings("unchecked")
    public DogTagCaseMenu(int containerId, Inventory playerInventory, ItemStack caseStack, int lockedSlot) {
        super((MenuType<DogTagCaseMenu>) KillConfirmed.dogTagCaseMenuType(), containerId);
        this.playerInventory = playerInventory;
        this.caseStack = caseStack;
        this.serverSide = true;
        this.backingContainer = new SimpleContainer(DogTagCaseModel.CAPACITY);
        this.clientWindow = new SimpleContainer(DogTagCaseModel.VISIBLE_SLOTS);
        DogTagCaseStorage.loadInto(caseStack, this.backingContainer, this.entryOrders, playerInventory.player.registryAccess());
        this.data = new SimpleContainerData(5);
        this.data.set(DATA_SCROLL_ROW, 0);
        this.data.set(DATA_AUTO_PICKUP, DogTagCaseStorage.isAutoPickup(caseStack) ? 1 : 0);
        this.data.set(DATA_SORT_MODE, DogTagCaseStorage.sortMode(caseStack).ordinal());
        this.data.set(DATA_FILLED_COUNT, countFilled());
        this.data.set(DATA_LOCKED_SLOT, lockedSlot);
        this.addDataSlots(this.data);
        buildSlots();
    }

    private void buildSlots() {
        // 9 x 6 visible case slots
        for (int row = 0; row < DogTagCaseModel.VISIBLE_ROWS; row++) {
            for (int col = 0; col < DogTagCaseModel.COLUMNS; col++) {
                int visibleIdx = row * DogTagCaseModel.COLUMNS + col;
                this.addSlot(new CaseWindowSlot(visibleIdx, 8 + col * 18, 18 + row * 18));
            }
        }
        // Standard 3 x 9 player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int invIdx = col + row * 9 + 9;
                this.addSlot(new LockablePlayerSlot(this.playerInventory, invIdx, 8 + col * 18, 140 + row * 18));
            }
        }
        // Standard 1 x 9 hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new LockablePlayerSlot(this.playerInventory, col, 8 + col * 18, 198));
        }
    }

    public int scrollRow() {
        return serverSide ? scrollRow : DogTagCaseModel.clampScrollRow(data.get(DATA_SCROLL_ROW));
    }

    public boolean isAutoPickup() {
        return data.get(DATA_AUTO_PICKUP) != 0;
    }

    public SortMode sortMode() {
        return SortMode.fromOrdinal(data.get(DATA_SORT_MODE));
    }

    public int filledCount() {
        return data.get(DATA_FILLED_COUNT);
    }

    public int lockedInventorySlot() {
        return data.get(DATA_LOCKED_SLOT);
    }

    public ItemStack findCaseStack() {
        if (serverSide) return caseStack;
        int slot = lockedInventorySlot();
        if (slot == Inventory.SLOT_OFFHAND) return playerInventory.player.getOffhandItem();
        if (slot >= 0 && slot < playerInventory.getContainerSize()) {
            return playerInventory.getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!serverSide) return false;
        HolderLookup.Provider registries = player.registryAccess();
        if (id >= BUTTON_SCROLL_BASE && id <= DogTagCaseModel.MAX_SCROLL_ROW) {
            this.scrollRow = DogTagCaseModel.clampScrollRow(id);
            this.data.set(DATA_SCROLL_ROW, this.scrollRow);
            this.broadcastChanges();
            return true;
        }
        if (id == BUTTON_TOGGLE_AUTO_PICKUP) {
            boolean enabled = DogTagCaseStorage.toggleAutoPickup(caseStack);
            this.data.set(DATA_AUTO_PICKUP, enabled ? 1 : 0);
            playerInventory.setChanged();
            this.broadcastChanges();
            return true;
        }
        if (id == BUTTON_CYCLE_SORT) {
            SortMode next = DogTagCaseStorage.cycleSortAndCompact(caseStack, backingContainer, entryOrders, registries);
            this.data.set(DATA_SORT_MODE, next.ordinal());
            this.data.set(DATA_FILLED_COUNT, countFilled());
            playerInventory.setChanged();
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && button == lockedInventorySlot()) {
            return;
        }
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot instanceof LockablePlayerSlot lockable && lockable.isLocked()) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        if (slot instanceof LockablePlayerSlot lockable && lockable.isLocked()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        if (index < DogTagCaseModel.VISIBLE_SLOTS) {
            // Case -> player inventory
            if (!this.moveItemStackTo(source, DogTagCaseModel.VISIBLE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (serverSide) persistBacking();
        } else {
            // Player inventory -> any free slot in all 108 backing slots
            if (!DogTagCaseStorage.isDogTag(source)) return ItemStack.EMPTY;
            if (serverSide) {
                boolean inserted = false;
                for (int i = 0; i < DogTagCaseModel.CAPACITY; i++) {
                    if (backingContainer.getItem(i).isEmpty()) {
                        backingContainer.setItem(i, source.split(1));
                        entryOrders[i] = 0L;
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) return ItemStack.EMPTY;
                persistBacking();
                this.broadcastChanges();
            } else {
                if (!this.moveItemStackTo(source, 0, DogTagCaseModel.VISIBLE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!serverSide) return true;
        int slot = lockedInventorySlot();
        ItemStack current = slot == Inventory.SLOT_OFFHAND
                ? player.getOffhandItem()
                : (slot >= 0 && slot < playerInventory.getContainerSize() ? playerInventory.getItem(slot) : ItemStack.EMPTY);
        return current == caseStack && DogTagCaseStorage.isDogTagCase(current);
    }

    private void persistBacking() {
        if (!serverSide) return;
        DogTagCaseStorage.saveFrom(caseStack, backingContainer, entryOrders, playerInventory.player.registryAccess());
        this.data.set(DATA_FILLED_COUNT, countFilled());
        this.playerInventory.setChanged();
    }

    private int countFilled() {
        int count = 0;
        for (int i = 0; i < DogTagCaseModel.CAPACITY; i++) {
            if (!backingContainer.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    private final class CaseWindowSlot extends Slot {
        private final int visibleIndex;

        CaseWindowSlot(int visibleIndex, int x, int y) {
            super(serverSide ? backingContainer : clientWindow, visibleIndex, x, y);
            this.visibleIndex = visibleIndex;
        }

        private int backingIndex() {
            return DogTagCaseModel.slotToEntryIndex(visibleIndex, scrollRow);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return DogTagCaseStorage.isDogTag(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public ItemStack getItem() {
            return serverSide ? backingContainer.getItem(backingIndex()) : clientWindow.getItem(visibleIndex);
        }

        @Override
        public void set(ItemStack stack) {
            if (serverSide) {
                int idx = backingIndex();
                backingContainer.setItem(idx, stack);
                if (stack.isEmpty()) entryOrders[idx] = 0L;
                persistBacking();
            } else {
                clientWindow.setItem(visibleIndex, stack);
            }
            this.setChanged();
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            if (serverSide && !newStack.isEmpty()) {
                entryOrders[backingIndex()] = 0L;
            }
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public ItemStack remove(int amount) {
            if (serverSide) {
                int idx = backingIndex();
                ItemStack removed = backingContainer.removeItem(idx, amount);
                if (backingContainer.getItem(idx).isEmpty()) entryOrders[idx] = 0L;
                persistBacking();
                return removed;
            }
            return clientWindow.removeItem(visibleIndex, amount);
        }

        @Override
        public void setChanged() {
            if (serverSide) persistBacking();
        }
    }

    private final class LockablePlayerSlot extends Slot {
        private final int invSlot;

        LockablePlayerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.invSlot = slot;
        }

        boolean isLocked() {
            return invSlot == lockedInventorySlot();
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isLocked();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isLocked();
        }
    }
}
