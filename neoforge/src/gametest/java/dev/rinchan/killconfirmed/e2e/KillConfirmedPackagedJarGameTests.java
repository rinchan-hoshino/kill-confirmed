package dev.rinchan.killconfirmed.e2e;

import com.mojang.authlib.GameProfile;
import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.KillConfirmedConfig;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Packaged-JAR black-box E2E GameTest suite running on real Minecraft 1.21.1 NeoForge.
 * All interactions go through real registries, real ServerPlayer.die(), real lava/fire ticks,
 * real Bundle inventory clicks, and real DogTagCaseItem.use() + AbstractContainerMenu methods.
 */
@GameTestHolder("kill_confirmed_e2e")
@PrefixGameTestTemplate(false)
public final class KillConfirmedPackagedJarGameTests {
    private static final ResourceLocation DOG_TAG_ID = ResourceLocation.fromNamespaceAndPath("kill_confirmed", "dog_tag");
    private static final ResourceLocation DOG_TAG_CASE_ID = ResourceLocation.fromNamespaceAndPath("kill_confirmed", "dog_tag_case");
    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void packagedJarIsLoadedFromRealJar(GameTestHelper helper) {
        var container = ModList.get().getModContainerById("kill_confirmed")
                .orElseThrow(() -> new AssertionError("kill_confirmed mod is not loaded"));
        String path = container.getModInfo().getOwningFile().getFile().getFilePath().toString();
        if (!path.endsWith(".jar")) {
            helper.fail("Expected kill_confirmed to be loaded from packaged .jar, got: " + path);
            return;
        }
        Item dogTag = BuiltInRegistries.ITEM.get(DOG_TAG_ID);
        Item dogTagCase = BuiltInRegistries.ITEM.get(DOG_TAG_CASE_ID);
        if (dogTag == Items.AIR || dogTagCase == Items.AIR) {
            helper.fail("Expected kill_confirmed:dog_tag and kill_confirmed:dog_tag_case to be registered");
            return;
        }
        if (!BuiltInRegistries.MENU.containsKey(DOG_TAG_CASE_ID)) {
            helper.fail("Expected kill_confirmed:dog_tag_case menu type to be registered");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void realServerPlayerDieDropsDogTagWithThreeLineEpitaphAndLandmark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item dogTagItem = BuiltInRegistries.ITEM.get(DOG_TAG_ID);
        BlockPos absPos = helper.absolutePos(new BlockPos(2, 2, 2));

        ServerPlayer victim = createRealServerPlayer(level, UUID.randomUUID(), "VictimAlpha", absPos);
        victim.experienceLevel = 42;
        victim.die(level.damageSources().cactus());

        List<ItemEntity> droppedTags = findDogTagsNear(level, absPos, dogTagItem);
        if (droppedTags.size() != 1) {
            helper.fail("Expected exactly 1 dropped DogTag after ServerPlayer.die(), found " + droppedTags.size());
            return;
        }

        ItemStack stack = droppedTags.getFirst().getItem();
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName == null || !customName.getString().contains("VictimAlpha")) {
            helper.fail("Expected DogTag custom name to contain VictimAlpha, got: " + customName);
            return;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().size() != 3) {
            helper.fail("Expected 3-line restrained epitaph lore, got: " + (lore == null ? "null" : lore.lines().size()));
            return;
        }
        String line0 = lore.lines().get(0).getString();
        String line1 = lore.lines().get(1).getString();
        String line2 = lore.lines().get(2).getString();

        if (!line0.contains("VictimAlpha")) {
            helper.fail("Expected line 0 death message to mention VictimAlpha, got: " + line0);
            return;
        }
        if (!line1.contains("⌖") || !line1.contains("(" + absPos.getX() + ", " + absPos.getY() + ", " + absPos.getZ() + ")")) {
            helper.fail("Expected line 1 to contain landmark marker and coordinates, got: " + line1);
            return;
        }
        if (!line2.contains("✦") || !line2.contains("42")) {
            helper.fail("Expected line 2 to contain level 42, got: " + line2);
            return;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            helper.fail("Expected CUSTOM_DATA snapshot on dropped DogTag");
            return;
        }
        CompoundTag snapshot = customData.copyTag().getCompound("kill_confirmed:snapshot");
        if (!"VictimAlpha".equals(snapshot.getString("owner_name"))
                || snapshot.getString("location_id").isBlank()
                || snapshot.getInt("experience_level") != 42) {
            helper.fail("Unexpected snapshot NBT: " + snapshot);
            return;
        }

        droppedTags.forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void dogTagAndCaseSurviveLavaAndFireTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item dogTagItem = BuiltInRegistries.ITEM.get(DOG_TAG_ID);
        Item dogTagCaseItem = BuiltInRegistries.ITEM.get(DOG_TAG_CASE_ID);

        // Build a 1x1 lava pit at (2, 1, 2) surrounded by stone walls
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.LAVA);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);

        Vec3 lavaCenter = helper.absoluteVec(new Vec3(2.5, 1.5, 2.5));
        ItemEntity tagEntity = new ItemEntity(level, lavaCenter.x, lavaCenter.y, lavaCenter.z, new ItemStack(dogTagItem));
        tagEntity.setDeltaMovement(Vec3.ZERO);
        ItemEntity caseEntity = new ItemEntity(level, lavaCenter.x, lavaCenter.y, lavaCenter.z, new ItemStack(dogTagCaseItem));
        caseEntity.setDeltaMovement(Vec3.ZERO);
        ItemEntity controlPaper = new ItemEntity(level, lavaCenter.x, lavaCenter.y, lavaCenter.z, new ItemStack(Items.PAPER));
        controlPaper.setDeltaMovement(Vec3.ZERO);

        level.addFreshEntity(tagEntity);
        level.addFreshEntity(caseEntity);
        level.addFreshEntity(controlPaper);

        helper.runAfterDelay(25, () -> {
            if (!tagEntity.isAlive() || tagEntity.isRemoved() || !tagEntity.fireImmune()) {
                helper.fail("DogTag burned in lava! alive=" + tagEntity.isAlive() + " fireImmune=" + tagEntity.fireImmune());
                return;
            }
            if (!caseEntity.isAlive() || caseEntity.isRemoved() || !caseEntity.fireImmune()) {
                helper.fail("DogTagCase burned in lava! alive=" + caseEntity.isAlive() + " fireImmune=" + caseEntity.fireImmune());
                return;
            }
            if (controlPaper.isAlive()) {
                helper.fail("Control paper item unexpectedly survived lava — lava check invalid");
                return;
            }
            tagEntity.discard();
            caseEntity.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void antiFarmCooldownBlocksRapidRepeatedDeaths(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item dogTagItem = BuiltInRegistries.ITEM.get(DOG_TAG_ID);
        BlockPos absPos = helper.absolutePos(new BlockPos(2, 2, 2));
        KillConfirmedConfig original = KillConfirmed.config();
        try {
            KillConfirmedConfig withCooldown = new KillConfirmedConfig(
                    original.dropStrategy(),
                    original.playerKillsOnly(),
                    original.fireResistant(),
                    60,
                    original.enableDogTagCaseRecipe(),
                    original.enableCaseLoot(),
                    original.enableWandererTrade(),
                    original.function(),
                    original.loreLines(),
                    original.loreTemplate());
            KillConfirmed.saveConfig(withCooldown);

            UUID repeatedVictimId = UUID.randomUUID();
            ServerPlayer death1 = createRealServerPlayer(level, repeatedVictimId, "FarmVictim", absPos);
            death1.die(level.damageSources().cactus());

            List<ItemEntity> afterFirst = findDogTagsNear(level, absPos, dogTagItem);
            if (afterFirst.size() != 1) {
                helper.fail("Expected 1 DogTag on first death, got " + afterFirst.size());
                return;
            }
            afterFirst.forEach(ItemEntity::discard);

            // Second death within 60s cooldown for same player UUID must be intercepted
            ServerPlayer death2 = createRealServerPlayer(level, repeatedVictimId, "FarmVictim", absPos);
            death2.die(level.damageSources().cactus());

            List<ItemEntity> afterSecond = findDogTagsNear(level, absPos, dogTagItem);
            if (!afterSecond.isEmpty()) {
                helper.fail("Anti-farm cooldown failed to block rapid second death: dropped " + afterSecond.size());
                return;
            }
            helper.succeed();
        } finally {
            KillConfirmed.saveConfig(original);
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void bundleStyleInventoryInteractionInsertsMultipleTagsIntoDogTagCase(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item dogTagItem = BuiltInRegistries.ITEM.get(DOG_TAG_ID);
        Item dogTagCaseItem = BuiltInRegistries.ITEM.get(DOG_TAG_CASE_ID);
        BlockPos absPos = helper.absolutePos(new BlockPos(2, 2, 2));

        ServerPlayer actor = createRealServerPlayer(level, UUID.randomUUID(), "Collector", absPos);
        ItemStack caseStack = new ItemStack(dogTagCaseItem);

        // Produce 5 real DogTags via real ServerPlayer.die()
        List<ItemStack> tags = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tags.add(produceRealDogTag(level, absPos, dogTagItem, "BundleVictim" + i, 10 + i));
        }

        // 1) Cursor holds DogTagCase, right-click 3 slots containing DogTags (overrideStackedOnOther)
        SimpleContainer inv = new SimpleContainer(9);
        for (int i = 0; i < 3; i++) {
            inv.setItem(i, tags.get(i));
            Slot slot = new Slot(inv, i, 0, 0);
            boolean handled = caseStack.overrideStackedOnOther(slot, ClickAction.SECONDARY, actor);
            if (!handled || !slot.getItem().isEmpty()) {
                helper.fail("overrideStackedOnOther failed to insert tag #" + i);
                return;
            }
        }

        // 2) Cursor holds DogTag, right-click DogTagCase in a slot (overrideOtherStackedOnMe)
        inv.setItem(4, caseStack);
        Slot caseSlot = new Slot(inv, 4, 0, 0);
        for (int i = 3; i < 5; i++) {
            ItemStack carried = tags.get(i);
            ItemStack[] cursorBox = new ItemStack[] { carried };
            SlotAccess access = new SlotAccess() {
                @Override
                public ItemStack get() { return cursorBox[0]; }
                @Override
                public boolean set(ItemStack s) { cursorBox[0] = s; return true; }
            };
            boolean handled = caseStack.overrideOtherStackedOnMe(carried, caseSlot, ClickAction.SECONDARY, actor, access);
            if (!handled || !carried.isEmpty()) {
                helper.fail("overrideOtherStackedOnMe failed to insert tag #" + i);
                return;
            }
        }

        if (readCaseCount(caseStack) != 5) {
            helper.fail("Expected 5 stored tags in DogTagCase after Bundle interactions, got " + readCaseCount(caseStack));
            return;
        }

        // 3) Right-click empty slot with DogTagCase pops last tag cleanly
        Slot emptySlot = new Slot(inv, 5, 0, 0);
        boolean popped = caseStack.overrideStackedOnOther(emptySlot, ClickAction.SECONDARY, actor);
        if (!popped || emptySlot.getItem().getItem() != dogTagItem || readCaseCount(caseStack) != 4) {
            helper.fail("Pop from DogTagCase failed: popped=" + popped + " count=" + readCaseCount(caseStack));
            return;
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void dogTagCaseMenuScrollButtonsAndShiftQuickMoveStress(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item dogTagItem = BuiltInRegistries.ITEM.get(DOG_TAG_ID);
        Item dogTagCaseItem = BuiltInRegistries.ITEM.get(DOG_TAG_CASE_ID);
        BlockPos absPos = helper.absolutePos(new BlockPos(2, 2, 2));

        ServerPlayer player = createRealServerPlayer(level, UUID.randomUUID(), "CaseOperator", absPos);
        ItemStack caseStack = new ItemStack(dogTagCaseItem);

        // Fill case with 65 real DogTags (spanning past 54-slot window into rows 6..11)
        SimpleContainer staging = new SimpleContainer(1);
        Slot stagingSlot = new Slot(staging, 0, 0, 0);
        for (int i = 0; i < 65; i++) {
            ItemStack tag = produceRealDogTag(level, absPos, dogTagItem, String.format("Victim_%02d", i), (i * 7) % 100);
            staging.setItem(0, tag);
            if (!caseStack.overrideStackedOnOther(stagingSlot, ClickAction.SECONDARY, player)) {
                helper.fail("Failed to stage tag #" + i + " into DogTagCase");
                return;
            }
        }
        if (readCaseCount(caseStack) != 65) {
            helper.fail("Expected 65 tags staged, got " + readCaseCount(caseStack));
            return;
        }

        // Open DogTagCaseMenu through real Item.use() (black-box, zero direct menu construction)
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, caseStack);
        caseStack.use(level, player, InteractionHand.MAIN_HAND);

        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu || menu.slots.size() != 90) {
            helper.fail("Expected 90-slot DogTagCaseMenu to open via Item.use(), got slots=" + menu.slots.size());
            return;
        }

        // 1) Verify 9x6 window scroll mapping to 108 backing slots
        String slot0AtRow0 = readOwnerName(menu.slots.get(0).getItem());
        if (!menu.clickMenuButton(player, 6)) {
            helper.fail("clickMenuButton(scrollRow=6) returned false");
            return;
        }
        String slot0AtRow6 = readOwnerName(menu.slots.get(0).getItem());
        ItemStack slot10AtRow6 = menu.slots.get(10).getItem(); // backing index 54 + 10 = 64 (last filled)
        ItemStack slot11AtRow6 = menu.slots.get(11).getItem(); // backing index 54 + 11 = 65 (empty)
        if (slot0AtRow0.equals(slot0AtRow6) || slot0AtRow6.isEmpty()) {
            helper.fail("Scroll row 6 did not shift window: row0=" + slot0AtRow0 + " row6=" + slot0AtRow6);
            return;
        }
        if (slot10AtRow6.isEmpty() || !slot11AtRow6.isEmpty()) {
            helper.fail("Backing boundary mismatch at row 6: slot64Empty=" + slot10AtRow6.isEmpty()
                    + " slot65Empty=" + slot11AtRow6.isEmpty());
            return;
        }
        menu.clickMenuButton(player, 0);

        // 2) Verify auto-pickup toggle (button 10) + real ItemEntity.playerTouch() auto-insertion
        if (!menu.clickMenuButton(player, 10)) {
            helper.fail("clickMenuButton(BUTTON_TOGGLE_AUTO_PICKUP=10) returned false");
            return;
        }
        ItemStack extraTag = produceRealDogTag(level, absPos, dogTagItem, "AutoPickedVictim", 99);
        ItemEntity groundTag = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), extraTag);
        groundTag.setNoPickUpDelay();
        level.addFreshEntity(groundTag);
        groundTag.playerTouch(player);
        if (!groundTag.isRemoved() || readCaseCount(caseStack) != 66) {
            helper.fail("Auto-pickup into DogTagCase failed: removed=" + groundTag.isRemoved()
                    + " caseCount=" + readCaseCount(caseStack));
            return;
        }

        // Re-open menu so backing container reflects the newly auto-picked 66th tag
        player.closeContainer();
        caseStack.use(level, player, InteractionHand.MAIN_HAND);
        menu = player.containerMenu;

        // 3) Verify 3-way sort cycling (button 11: NEWEST -> BY_VICTIM -> BY_LEVEL -> NEWEST)
        for (int s = 0; s < 3; s++) {
            if (!menu.clickMenuButton(player, 11)) {
                helper.fail("clickMenuButton(BUTTON_CYCLE_SORT=11) failed at cycle " + s);
                return;
            }
            if (readCaseCount(caseStack) != 66) {
                helper.fail("Sort cycle " + s + " changed tag count to " + readCaseCount(caseStack));
                return;
            }
        }

        // 4) Shift quickMoveStack rapid-click stress across multiple scroll rows
        int expectedTotal = 66;
        // Rapidly shift-click 20 tags out of the case into player inventory
        for (int i = 0; i < 20; i++) {
            menu.clickMenuButton(player, i % 7);
            menu.quickMoveStack(player, i % 54);
        }
        // Rapidly shift-click player inventory slots (54..89) back into the case (including locked slot!)
        for (int round = 0; round < 2; round++) {
            for (int invMenuSlot = 54; invMenuSlot < 90; invMenuSlot++) {
                menu.quickMoveStack(player, invMenuSlot);
            }
        }

        int inCase = readCaseCount(caseStack);
        int inPlayerInv = countDogTagsInInventory(player, dogTagItem);
        if (inCase + inPlayerInv != expectedTotal) {
            helper.fail("Shift quickMoveStack violated conservation! expected=" + expectedTotal
                    + " actual=" + (inCase + inPlayerInv) + " (case=" + inCase + ", inv=" + inPlayerInv + ")");
            return;
        }
        // Locked case slot (hotbar 0) must still hold the DogTagCase
        if (player.getInventory().getItem(0) != caseStack) {
            helper.fail("Locked DogTagCase slot was modified during quickMoveStack stress!");
            return;
        }

        player.closeContainer();
        helper.succeed();
    }

    private static ServerPlayer createRealServerPlayer(ServerLevel level, UUID uuid, String name, BlockPos pos) {
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(uuid, name);
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(
                server,
                connection,
                player,
                CommonListenerCookie.createInitial(profile, false));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }

    private static ItemStack produceRealDogTag(
            ServerLevel level, BlockPos pos, Item dogTagItem, String victimName, int xpLevel) {
        ServerPlayer victim = createRealServerPlayer(level, UUID.randomUUID(), victimName, pos);
        victim.experienceLevel = xpLevel;
        victim.die(level.damageSources().cactus());
        List<ItemEntity> entities = findDogTagsNear(level, pos, dogTagItem);
        if (entities.isEmpty()) {
            throw new AssertionError("ServerPlayer.die() did not drop a DogTag for " + victimName);
        }
        ItemStack stack = entities.getLast().getItem().copy();
        entities.forEach(ItemEntity::discard);
        return stack;
    }

    private static List<ItemEntity> findDogTagsNear(ServerLevel level, BlockPos center, Item dogTagItem) {
        AABB box = new AABB(center).inflate(3.0);
        return level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.isAlive() && e.getItem().getItem() == dogTagItem);
    }

    private static int readCaseCount(ItemStack caseStack) {
        CustomData data = caseStack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        CompoundTag root = data.copyTag();
        if (!root.contains("kill_confirmed:case", Tag.TAG_COMPOUND)) return 0;
        return root.getCompound("kill_confirmed:case").getList("Items", Tag.TAG_COMPOUND).size();
    }

    private static String readOwnerName(ItemStack dogTag) {
        if (dogTag.isEmpty()) return "";
        CustomData data = dogTag.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        return data.copyTag().getCompound("kill_confirmed:snapshot").getString("owner_name");
    }

    private static int countDogTagsInInventory(ServerPlayer player, Item dogTagItem) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == dogTagItem) {
                count += s.getCount();
            }
        }
        return count;
    }
}
