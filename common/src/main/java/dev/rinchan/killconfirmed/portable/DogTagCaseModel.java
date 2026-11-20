package dev.rinchan.killconfirmed.portable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure-Java model for the 108-slot Dog Tag Case: pagination, search, sorting, stats, and loot targets. */
public final class DogTagCaseModel {
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 6;
    public static final int TOTAL_ROWS = 12;
    public static final int CAPACITY = COLUMNS * TOTAL_ROWS; // 108
    public static final int VISIBLE_SLOTS = COLUMNS * VISIBLE_ROWS; // 54
    public static final int MAX_SCROLL_ROW = TOTAL_ROWS - VISIBLE_ROWS; // 6

    public static final float STRUCTURE_LOOT_CHANCE = 0.08f;
    public static final int WANDERER_EMERALD_COST = 20;

    private static final Set<String> ELIGIBLE_LOOT_TABLES = Set.of(
            "minecraft:chests/ancient_city",
            "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing",
            "minecraft:chests/stronghold_library",
            "minecraft:chests/bastion_treasure",
            "minecraft:chests/bastion_other",
            "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/woodland_mansion"
    );

    private DogTagCaseModel() {}

    public enum SortMode {
        NEWEST("kill_confirmed.case.sort.newest"),
        BY_VICTIM("kill_confirmed.case.sort.victim"),
        BY_LEVEL("kill_confirmed.case.sort.level");

        private final String translationKey;

        SortMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        public SortMode next() {
            SortMode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public static SortMode fromOrdinal(int ordinal) {
            SortMode[] all = values();
            if (ordinal < 0 || ordinal >= all.length) return NEWEST;
            return all[ordinal];
        }
    }

    public record EntryMeta(int slot, String ownerName, String searchableText, int level, long order) {}

    public record VictimCount(String name, int count) {}

    public static int clampScrollRow(int row) {
        return Math.max(0, Math.min(MAX_SCROLL_ROW, row));
    }

    public static int slotToEntryIndex(int visibleSlot, int scrollRow) {
        return clampScrollRow(scrollRow) * COLUMNS + visibleSlot;
    }

    public static boolean matchesSearch(String searchableText, String query) {
        if (query == null || query.isBlank()) return true;
        if (searchableText == null || searchableText.isEmpty()) return false;
        String haystack = searchableText.toLowerCase(Locale.ROOT);
        for (String token : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!token.isEmpty() && !haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public static List<EntryMeta> sortEntries(List<EntryMeta> entries, SortMode mode) {
        List<EntryMeta> sorted = new ArrayList<>(entries);
        Comparator<EntryMeta> comparator = switch (mode) {
            case NEWEST -> Comparator.comparingLong(EntryMeta::order).reversed()
                    .thenComparingInt(EntryMeta::slot);
            case BY_VICTIM -> Comparator.comparing(
                            (EntryMeta e) -> e.ownerName().toLowerCase(Locale.ROOT))
                    .thenComparing(Comparator.comparingLong(EntryMeta::order).reversed());
            case BY_LEVEL -> Comparator.comparingInt(EntryMeta::level).reversed()
                    .thenComparing(e -> e.ownerName().toLowerCase(Locale.ROOT))
                    .thenComparing(Comparator.comparingLong(EntryMeta::order).reversed());
        };
        sorted.sort(comparator);
        return List.copyOf(sorted);
    }

    public static List<VictimCount> topVictims(List<String> ownerNames, int limit) {
        if (ownerNames == null || ownerNames.isEmpty() || limit <= 0) return List.of();
        Map<String, Integer> counts = new HashMap<>();
        for (String name : ownerNames) {
            if (name != null && !name.isBlank()) {
                counts.merge(name, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new VictimCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(VictimCount::count).reversed()
                        .thenComparing(v -> v.name().toLowerCase(Locale.ROOT)))
                .limit(limit)
                .toList();
    }

    public static boolean isEligibleChestLoot(String lootTableId) {
        return lootTableId != null && ELIGIBLE_LOOT_TABLES.contains(lootTableId);
    }

    public static String humanizePath(String path) {
        if (path == null || path.isEmpty()) return "Unknown";
        String[] parts = path.split("[/_]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? path : sb.toString();
    }
}
