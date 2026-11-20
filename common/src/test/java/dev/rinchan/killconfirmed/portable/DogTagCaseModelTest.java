package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.EntryMeta;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel.SortMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class DogTagCaseModelTest {
    @Test
    void capacity_and_scroll_window_math_match_9x12_grid() {
        assertEquals(108, DogTagCaseModel.CAPACITY);
        assertEquals(54, DogTagCaseModel.VISIBLE_SLOTS);
        assertEquals(6, DogTagCaseModel.MAX_SCROLL_ROW);

        assertEquals(0, DogTagCaseModel.clampScrollRow(-3));
        assertEquals(6, DogTagCaseModel.clampScrollRow(99));
        assertEquals(0, DogTagCaseModel.slotToEntryIndex(0, 0));
        assertEquals(54, DogTagCaseModel.slotToEntryIndex(0, 6));
        assertEquals(107, DogTagCaseModel.slotToEntryIndex(53, 6));
    }

    @Test
    void search_supports_case_insensitive_multi_token_queries() {
        String text = "Steve's Dog Tag Steve was slain by Warden Ancient City Lv.42";
        assertTrue(DogTagCaseModel.matchesSearch(text, ""));
        assertTrue(DogTagCaseModel.matchesSearch(text, "warden"));
        assertTrue(DogTagCaseModel.matchesSearch(text, "steve ancient"));
        assertFalse(DogTagCaseModel.matchesSearch(text, "steve fortress"));
    }

    @Test
    void sort_modes_cycle_and_order_entries_deterministically() {
        assertEquals(SortMode.BY_VICTIM, SortMode.NEWEST.next());
        assertEquals(SortMode.BY_LEVEL, SortMode.BY_VICTIM.next());
        assertEquals(SortMode.NEWEST, SortMode.BY_LEVEL.next());

        var entries = List.of(
                new EntryMeta(0, "Charlie", "Charlie", 10, 1L),
                new EntryMeta(1, "Alice", "Alice", 55, 2L),
                new EntryMeta(2, "Bob", "Bob", 30, 3L));

        var newest = DogTagCaseModel.sortEntries(entries, SortMode.NEWEST);
        assertEquals(List.of("Bob", "Alice", "Charlie"), newest.stream().map(EntryMeta::ownerName).toList());

        var byVictim = DogTagCaseModel.sortEntries(entries, SortMode.BY_VICTIM);
        assertEquals(List.of("Alice", "Bob", "Charlie"), byVictim.stream().map(EntryMeta::ownerName).toList());

        var byLevel = DogTagCaseModel.sortEntries(entries, SortMode.BY_LEVEL);
        assertEquals(List.of("Alice", "Bob", "Charlie"), byLevel.stream().map(EntryMeta::ownerName).toList());
    }

    @Test
    void top_victims_aggregates_and_limits_correctly() {
        var owners = List.of("Alice", "Bob", "Alice", "Charlie", "Alice", "Bob", "Dave", "Eve", "Frank");
        var top5 = DogTagCaseModel.topVictims(owners, 5);
        assertEquals(5, top5.size());
        assertEquals("Alice", top5.get(0).name());
        assertEquals(3, top5.get(0).count());
        assertEquals("Bob", top5.get(1).name());
        assertEquals(2, top5.get(1).count());
        assertEquals(3, DogTagCaseModel.topVictims(owners, 3).size());
    }

    @Test
    void loot_targets_and_landmark_humanization() {
        assertTrue(DogTagCaseModel.isEligibleChestLoot("minecraft:chests/ancient_city"));
        assertTrue(DogTagCaseModel.isEligibleChestLoot("minecraft:chests/woodland_mansion"));
        assertFalse(DogTagCaseModel.isEligibleChestLoot("minecraft:chests/spawn_bonus_chest"));
        assertEquals("Ancient City", DogTagCaseModel.humanizePath("ancient_city"));
        assertEquals("Crimson Forest", DogTagCaseModel.humanizePath("crimson_forest"));
    }
}
