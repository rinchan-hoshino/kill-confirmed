package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import dev.rinchan.killconfirmed.portable.DogTagCaseModel.EntryMeta;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.SortMode;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.VictimCount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Property-based randomized invariant tests for the 108-slot Dog Tag Case model.
 * Uses deterministic fixed seeds for 100% reproducible CI execution.
 */
class DogTagCasePropertyTest {
    private static final int PLAYER_INV_SIZE = 36;
    private static final String[] VICTIMS = {
        "Steve", "Alex", "Herobrine", "Notch", "Rin", "Kestrel", "Valkyrie", "WardenSlayer"
    };
    private static final String[] LANDMARKS = {
        "Ancient City", "Bastion Remnant", "Stronghold", "Woodland Mansion", "Deep Dark", "Plains"
    };
    private static final String[] CAUSES = {
        "was slain by Warden", "fell from a high place", "tried to swim in lava", "was shot by Skeleton"
    };

    private record SimulatedDogTag(
            int uniqueId,
            String ownerName,
            int level,
            String landmark,
            String cause,
            String nbtPayload) {
        String searchableText() {
            return ownerName + "'s Dog Tag " + ownerName + " " + cause + " " + landmark + " Lv." + level;
        }
    }

    @Test
    void two_thousand_step_random_bombardment_conserves_count_and_nbt_strictly() {
        runBombardment(0x5EED_CAFE_108L, 2_000);
    }

    @Test
    void multi_seed_thousand_step_bombardment_zero_loss_zero_duplication() {
        long[] seeds = {108L, 20250101L, 0xDEADBEEFL, 0x123456789ABCL};
        for (long seed : seeds) {
            runBombardment(seed, 1_000);
        }
    }

    @Test
    void boundary_scroll_sort_and_search_fuzz_1500_iterations() {
        Random rng = new Random(0xB00B_0001_1500L);
        for (int iter = 0; iter < 1_500; iter++) {
            int rawRow = rng.nextInt(41) - 20; // [-20, 20]
            int clamped = DogTagCaseModel.clampScrollRow(rawRow);
            assertTrue(clamped >= 0 && clamped <= DogTagCaseModel.MAX_SCROLL_ROW,
                    "Clamped scroll row out of bounds: " + clamped);

            for (int visible = 0; visible < DogTagCaseModel.VISIBLE_SLOTS; visible++) {
                int backing = DogTagCaseModel.slotToEntryIndex(visible, rawRow);
                assertTrue(backing >= 0 && backing < DogTagCaseModel.CAPACITY,
                        "Backing slot out of bounds: " + backing);
            }

            int count = rng.nextInt(DogTagCaseModel.CAPACITY + 1);
            List<EntryMeta> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String owner = VICTIMS[rng.nextInt(VICTIMS.length)];
                int level = rng.nextInt(500);
                long order = rng.nextInt(50) + 1L;
                entries.add(new EntryMeta(i, owner, owner + " Lv." + level, level, order));
            }

            for (SortMode mode : SortMode.values()) {
                List<EntryMeta> sorted = DogTagCaseModel.sortEntries(entries, mode);
                assertEquals(count, sorted.size(), "Sort must preserve exact entry count");
                Set<Integer> seenSlots = new HashSet<>();
                for (EntryMeta e : sorted) {
                    assertTrue(seenSlots.add(e.slot()), "Sort produced duplicate slot " + e.slot());
                }
                for (int i = 1; i < sorted.size(); i++) {
                    EntryMeta prev = sorted.get(i - 1);
                    EntryMeta curr = sorted.get(i);
                    switch (mode) {
                        case NEWEST -> assertTrue(prev.order() >= curr.order());
                        case BY_VICTIM -> assertTrue(
                                prev.ownerName().toLowerCase(Locale.ROOT)
                                        .compareTo(curr.ownerName().toLowerCase(Locale.ROOT)) <= 0);
                        case BY_LEVEL -> assertTrue(prev.level() >= curr.level());
                    }
                }
            }

            // Search fuzzing including blank/whitespace/multi-token/unicode
            String[] queries = {
                "", "   ", "steve", "STEVE lv.", "ancient city", "nonexistent_xyz", "warden 42"
            };
            String q = queries[rng.nextInt(queries.length)];
            for (EntryMeta e : entries) {
                boolean matched = DogTagCaseModel.matchesSearch(e.searchableText(), q);
                if (q.isBlank()) {
                    assertTrue(matched, "Blank query must match everything");
                }
            }
        }
    }

    private void runBombardment(long seed, int steps) {
        Random rng = new Random(seed);
        SimulatedDogTag[] caseSlots = new SimulatedDogTag[DogTagCaseModel.CAPACITY];
        long[] entryOrders = new long[DogTagCaseModel.CAPACITY];
        SimulatedDogTag[] playerInv = new SimulatedDogTag[PLAYER_INV_SIZE];

        // Ground-truth oracle: every live tag across case + player inventory
        Map<Integer, SimulatedDogTag> liveOracle = new HashMap<>();
        int nextTagId = 1;
        long nextOrder = 0L;
        int scrollRow = 0;
        SortMode sortMode = SortMode.NEWEST;

        // Pre-seed a few tags in player inventory so immediate moves can happen
        for (int i = 0; i < 6; i++) {
            SimulatedDogTag tag = createTag(rng, nextTagId++);
            playerInv[i] = tag;
            liveOracle.put(tag.uniqueId(), tag);
        }

        for (int step = 0; step < steps; step++) {
            int op = rng.nextInt(100);
            if (op < 20) {
                // Op 1: Insert brand-new tag directly into case (Bundle/AutoPickup insertSingle)
                int freeSlot = firstEmpty(caseSlots);
                if (freeSlot >= 0) {
                    SimulatedDogTag tag = createTag(rng, nextTagId++);
                    caseSlots[freeSlot] = tag;
                    entryOrders[freeSlot] = ++nextOrder;
                    liveOracle.put(tag.uniqueId(), tag);
                } else {
                    assertEquals(DogTagCaseModel.CAPACITY, countNonNull(caseSlots),
                            "Insert failed only when case is strictly full (108)");
                }
            } else if (op < 35) {
                // Op 2: Pop last tag from case (Bundle popLast) into player inventory if space
                int playerFree = firstEmpty(playerInv);
                if (playerFree >= 0) {
                    int lastIdx = lastNonEmpty(caseSlots);
                    if (lastIdx >= 0) {
                        SimulatedDogTag popped = caseSlots[lastIdx];
                        caseSlots[lastIdx] = null;
                        entryOrders[lastIdx] = 0L;
                        playerInv[playerFree] = popped;
                    }
                }
            } else if (op < 50) {
                // Op 3: Scroll wheel row (including out-of-range inputs clamped to 0..6)
                int raw = rng.nextInt(15) - 4; // [-4..10]
                scrollRow = DogTagCaseModel.clampScrollRow(raw);
                assertTrue(scrollRow >= 0 && scrollRow <= DogTagCaseModel.MAX_SCROLL_ROW);
            } else if (op < 65) {
                // Op 4: Cycle sort mode & compact (NEWEST -> BY_VICTIM -> BY_LEVEL)
                sortMode = sortMode.next();
                List<EntryMeta> metas = new ArrayList<>();
                SimulatedDogTag[] snapshot = Arrays.copyOf(caseSlots, caseSlots.length);
                long[] orderSnapshot = Arrays.copyOf(entryOrders, entryOrders.length);
                for (int i = 0; i < DogTagCaseModel.CAPACITY; i++) {
                    SimulatedDogTag t = caseSlots[i];
                    if (t == null) continue;
                    metas.add(new EntryMeta(i, t.ownerName(), t.searchableText(), t.level(), entryOrders[i]));
                }
                List<EntryMeta> sorted = DogTagCaseModel.sortEntries(metas, sortMode);
                Arrays.fill(caseSlots, null);
                Arrays.fill(entryOrders, 0L);
                for (int i = 0; i < sorted.size(); i++) {
                    int origSlot = sorted.get(i).slot();
                    caseSlots[i] = snapshot[origSlot];
                    entryOrders[i] = orderSnapshot[origSlot];
                }
                assertEquals(metas.size(), countNonNull(caseSlots));
            } else if (op < 80) {
                // Op 5: Shift quick-move Case (visible slot 0..53 mapped via scrollRow) -> Player Inventory
                int visibleSlot = rng.nextInt(DogTagCaseModel.VISIBLE_SLOTS);
                int backingIdx = DogTagCaseModel.slotToEntryIndex(visibleSlot, scrollRow);
                assertTrue(backingIdx >= 0 && backingIdx < DogTagCaseModel.CAPACITY);
                if (caseSlots[backingIdx] != null) {
                    int invFree = firstEmpty(playerInv);
                    if (invFree >= 0) {
                        playerInv[invFree] = caseSlots[backingIdx];
                        caseSlots[backingIdx] = null;
                        entryOrders[backingIdx] = 0L;
                    }
                }
            } else if (op < 92) {
                // Op 6: Shift quick-move Player Inventory -> Case (any free slot in 0..107)
                int invSlot = rng.nextInt(PLAYER_INV_SIZE);
                if (playerInv[invSlot] != null) {
                    int caseFree = firstEmpty(caseSlots);
                    if (caseFree >= 0) {
                        caseSlots[caseFree] = playerInv[invSlot];
                        entryOrders[caseFree] = ++nextOrder;
                        playerInv[invSlot] = null;
                    }
                } else if (countNonNull(playerInv) < PLAYER_INV_SIZE / 2) {
                    // Occasionally replenish player inventory to keep transfers active
                    SimulatedDogTag tag = createTag(rng, nextTagId++);
                    playerInv[invSlot] = tag;
                    liveOracle.put(tag.uniqueId(), tag);
                }
            } else {
                // Op 7: Search filtering & Top-5 casualty stats over current visible window
                String query = rng.nextBoolean()
                        ? VICTIMS[rng.nextInt(VICTIMS.length)]
                        : (VICTIMS[rng.nextInt(VICTIMS.length)] + " " + LANDMARKS[rng.nextInt(LANDMARKS.length)]);
                int matchedVisible = 0;
                for (int vis = 0; vis < DogTagCaseModel.VISIBLE_SLOTS; vis++) {
                    int idx = DogTagCaseModel.slotToEntryIndex(vis, scrollRow);
                    SimulatedDogTag tag = caseSlots[idx];
                    if (tag != null && DogTagCaseModel.matchesSearch(tag.searchableText(), query)) {
                        matchedVisible++;
                    }
                }
                assertTrue(matchedVisible >= 0 && matchedVisible <= DogTagCaseModel.VISIBLE_SLOTS);

                List<String> owners = new ArrayList<>();
                for (SimulatedDogTag t : caseSlots) {
                    if (t != null) owners.add(t.ownerName());
                }
                List<VictimCount> top5 = DogTagCaseModel.topVictims(owners, 5);
                assertTrue(top5.size() <= 5);
                int topSum = top5.stream().mapToInt(VictimCount::count).sum();
                assertTrue(topSum <= owners.size());
                for (int i = 1; i < top5.size(); i++) {
                    assertTrue(top5.get(i - 1).count() >= top5.get(i).count());
                }
            }

            // Strict conservation check after EVERY single step
            assertStrictConservation(step, caseSlots, playerInv, liveOracle);
        }
    }

    private static void assertStrictConservation(
            int step,
            SimulatedDogTag[] caseSlots,
            SimulatedDogTag[] playerInv,
            Map<Integer, SimulatedDogTag> liveOracle) {
        Map<Integer, SimulatedDogTag> observed = new HashMap<>();
        int caseCount = 0;
        for (int i = 0; i < caseSlots.length; i++) {
            SimulatedDogTag tag = caseSlots[i];
            if (tag != null) {
                caseCount++;
                assertNull(observed.put(tag.uniqueId(), tag),
                        "Duplicated dog tag id=" + tag.uniqueId() + " in case slot " + i + " at step " + step);
            }
        }
        assertTrue(caseCount <= DogTagCaseModel.CAPACITY, "Case capacity exceeded at step " + step);

        for (int i = 0; i < playerInv.length; i++) {
            SimulatedDogTag tag = playerInv[i];
            if (tag != null) {
                assertNull(observed.put(tag.uniqueId(), tag),
                        "Duplicated dog tag id=" + tag.uniqueId() + " in player inv slot " + i + " at step " + step);
            }
        }

        assertEquals(liveOracle.size(), observed.size(),
                "Dog tag count not conserved at step " + step);
        for (var entry : liveOracle.entrySet()) {
            SimulatedDogTag actual = observed.get(entry.getKey());
            assertNotNull(actual, "Lost dog tag id=" + entry.getKey() + " at step " + step);
            assertEquals(entry.getValue().nbtPayload(), actual.nbtPayload(),
                    "NBT payload mutated for dog tag id=" + entry.getKey() + " at step " + step);
        }
    }

    private static SimulatedDogTag createTag(Random rng, int id) {
        String owner = VICTIMS[rng.nextInt(VICTIMS.length)];
        int level = rng.nextInt(120);
        String landmark = LANDMARKS[rng.nextInt(LANDMARKS.length)];
        String cause = CAUSES[rng.nextInt(CAUSES.length)];
        String nbt = "{schema:1,id:" + id + ",owner_name:\"" + owner + "\",level:" + level
                + ",location:\"" + landmark + "\",cause:\"" + cause + "\"}";
        return new SimulatedDogTag(id, owner, level, landmark, cause, nbt);
    }

    private static int firstEmpty(SimulatedDogTag[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null) return i;
        }
        return -1;
    }

    private static int lastNonEmpty(SimulatedDogTag[] arr) {
        for (int i = arr.length - 1; i >= 0; i--) {
            if (arr[i] != null) return i;
        }
        return -1;
    }

    private static int countNonNull(SimulatedDogTag[] arr) {
        int c = 0;
        for (SimulatedDogTag t : arr) {
            if (t != null) c++;
        }
        return c;
    }
}
