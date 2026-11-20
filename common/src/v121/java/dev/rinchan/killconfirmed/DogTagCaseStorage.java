package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.EntryMeta;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.SortMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Persistence and operations for the 108-slot Dog Tag Case (1.21.1). */
public final class DogTagCaseStorage {
    private static final String CASE_KEY = "kill_confirmed:case";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_ORDER = "Order";
    private static final String TAG_AUTO_PICKUP = "AutoPickup";
    private static final String TAG_SORT_MODE = "SortMode";
    private static final String TAG_NEXT_ORDER = "NextOrder";

    private DogTagCaseStorage() {}

    public static boolean isDogTag(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == KillConfirmed.dogTag();
    }

    public static boolean isDogTagCase(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == KillConfirmed.dogTagCase();
    }

    private static CompoundTag getCaseTag(ItemStack caseStack) {
        if (caseStack.isEmpty()) return new CompoundTag();
        CustomData data = caseStack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return new CompoundTag();
        CompoundTag root = data.copyTag();
        return root.contains(CASE_KEY, Tag.TAG_COMPOUND) ? root.getCompound(CASE_KEY) : new CompoundTag();
    }

    private static void saveCaseTag(ItemStack caseStack, CompoundTag caseTag) {
        CustomData existing = caseStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = existing != null ? existing.copyTag() : new CompoundTag();
        root.put(CASE_KEY, caseTag);
        caseStack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static int count(ItemStack caseStack) {
        return getCaseTag(caseStack).getList(TAG_ITEMS, Tag.TAG_COMPOUND).size();
    }

    public static boolean isAutoPickup(ItemStack caseStack) {
        return getCaseTag(caseStack).getBoolean(TAG_AUTO_PICKUP);
    }

    public static boolean toggleAutoPickup(ItemStack caseStack) {
        CompoundTag tag = getCaseTag(caseStack);
        boolean next = !tag.getBoolean(TAG_AUTO_PICKUP);
        tag.putBoolean(TAG_AUTO_PICKUP, next);
        saveCaseTag(caseStack, tag);
        return next;
    }

    public static SortMode sortMode(ItemStack caseStack) {
        return SortMode.fromOrdinal(getCaseTag(caseStack).getByte(TAG_SORT_MODE));
    }

    public static void loadInto(ItemStack caseStack, SimpleContainer container, long[] ordersOut, HolderLookup.Provider registries) {
        container.clearContent();
        if (ordersOut != null) {
            java.util.Arrays.fill(ordersOut, 0L);
        }
        CompoundTag tag = getCaseTag(caseStack);
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte(TAG_SLOT) & 0xFF;
            if (slot < 0 || slot >= DogTagCaseModel.CAPACITY) continue;
            ItemStack.CODEC.parse(ops, entry.getCompound(TAG_ITEM)).result().ifPresent(stack -> {
                if (isDogTag(stack)) {
                    container.setItem(slot, stack);
                    if (ordersOut != null) {
                        ordersOut[slot] = entry.getLong(TAG_ORDER);
                    }
                }
            });
        }
    }

    public static void saveFrom(ItemStack caseStack, SimpleContainer container, long[] orders, HolderLookup.Provider registries) {
        CompoundTag tag = getCaseTag(caseStack);
        long nextOrder = tag.getLong(TAG_NEXT_ORDER);
        ListTag list = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < DogTagCaseModel.CAPACITY; i++) {
            ItemStack stack = container.getItem(i);
            if (!isDogTag(stack)) continue;
            long order = (orders != null && orders[i] > 0) ? orders[i] : ++nextOrder;
            if (orders != null) orders[i] = order;
            final int slotIdx = i;
            final long finalOrder = order;
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(encoded -> {
                if (encoded instanceof CompoundTag itemNbt) {
                    CompoundTag entry = new CompoundTag();
                    entry.putByte(TAG_SLOT, (byte) slotIdx);
                    entry.put(TAG_ITEM, itemNbt);
                    entry.putLong(TAG_ORDER, finalOrder);
                    list.add(entry);
                }
            });
        }
        tag.put(TAG_ITEMS, list);
        tag.putLong(TAG_NEXT_ORDER, nextOrder);
        saveCaseTag(caseStack, tag);
    }

    public static boolean insertSingle(ItemStack caseStack, ItemStack dogTag, HolderLookup.Provider registries) {
        if (!isDogTagCase(caseStack) || !isDogTag(dogTag)) return false;
        SimpleContainer container = new SimpleContainer(DogTagCaseModel.CAPACITY);
        long[] orders = new long[DogTagCaseModel.CAPACITY];
        loadInto(caseStack, container, orders, registries);
        for (int i = 0; i < DogTagCaseModel.CAPACITY; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, dogTag.copyWithCount(1));
                orders[i] = 0L; // assigned nextOrder on save
                saveFrom(caseStack, container, orders, registries);
                return true;
            }
        }
        return false;
    }

    public static ItemStack popLast(ItemStack caseStack, HolderLookup.Provider registries) {
        if (!isDogTagCase(caseStack)) return ItemStack.EMPTY;
        SimpleContainer container = new SimpleContainer(DogTagCaseModel.CAPACITY);
        long[] orders = new long[DogTagCaseModel.CAPACITY];
        loadInto(caseStack, container, orders, registries);
        for (int i = DogTagCaseModel.CAPACITY - 1; i >= 0; i--) {
            ItemStack found = container.getItem(i);
            if (!found.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
                orders[i] = 0L;
                saveFrom(caseStack, container, orders, registries);
                return found;
            }
        }
        return ItemStack.EMPTY;
    }

    public static SortMode cycleSortAndCompact(
            ItemStack caseStack, SimpleContainer container, long[] orders, HolderLookup.Provider registries) {
        SortMode nextMode = sortMode(caseStack).next();
        CompoundTag tag = getCaseTag(caseStack);
        tag.putByte(TAG_SORT_MODE, (byte) nextMode.ordinal());
        saveCaseTag(caseStack, tag);

        List<EntryMeta> metas = new ArrayList<>();
        NonNullList<ItemStack> snapshot = NonNullList.withSize(DogTagCaseModel.CAPACITY, ItemStack.EMPTY);
        long[] orderSnapshot = new long[DogTagCaseModel.CAPACITY];
        for (int i = 0; i < DogTagCaseModel.CAPACITY; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            snapshot.set(i, stack.copy());
            orderSnapshot[i] = orders[i];
            String owner = DogTagSnapshotCodec.readOwnerName(stack).orElse(stack.getHoverName().getString());
            int level = DogTagSnapshotCodec.readExperienceLevel(stack);
            metas.add(new EntryMeta(i, owner, searchableText(stack), level, orders[i]));
        }
        List<EntryMeta> sorted = DogTagCaseModel.sortEntries(metas, nextMode);
        container.clearContent();
        java.util.Arrays.fill(orders, 0L);
        for (int idx = 0; idx < sorted.size(); idx++) {
            EntryMeta meta = sorted.get(idx);
            container.setItem(idx, snapshot.get(meta.slot()));
            orders[idx] = orderSnapshot[meta.slot()];
        }
        saveFrom(caseStack, container, orders, registries);
        return nextMode;
    }

    public static String searchableText(ItemStack stack) {
        if (stack.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(stack.getHoverName().getString());
        DogTagSnapshotCodec.readOwnerName(stack).ifPresent(name -> sb.append(' ').append(name));
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (var line : lore.lines()) {
                sb.append(' ').append(line.getString());
            }
        }
        return sb.toString();
    }

    /** Fast lightweight victim names extraction directly from NBT (works on both client & server). */
    public static List<String> readOwnerNames(ItemStack caseStack) {
        CompoundTag tag = getCaseTag(caseStack);
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        List<String> owners = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemNbt = list.getCompound(i).getCompound(TAG_ITEM);
            CompoundTag components = itemNbt.getCompound("components");
            CompoundTag customData = components.getCompound("minecraft:custom_data");
            CompoundTag snapshot = customData.getCompound(DogTagSnapshotCodec.ROOT_KEY);
            String owner = snapshot.getString("owner_name");
            if (!owner.isBlank()) {
                owners.add(owner);
            }
        }
        return owners;
    }

    public static boolean tryAutoPickup(ServerPlayer player, ItemStack pickedStack) {
        if (!isDogTag(pickedStack)) return false;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack candidate = inventory.getItem(i);
            if (!isDogTagCase(candidate) || !isAutoPickup(candidate)) continue;
            if (count(candidate) >= DogTagCaseModel.CAPACITY) continue;
            if (insertSingle(candidate, pickedStack, player.registryAccess())) {
                pickedStack.shrink(1);
                inventory.setChanged();
                player.level().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.BUNDLE_INSERT,
                        SoundSource.PLAYERS,
                        0.6f,
                        1.0f);
                return true;
            }
        }
        return false;
    }
}
