package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeathCooldownTrackerTest {
    @Test
    void zero_cooldown_always_allows_drops() {
        var tracker = new DeathCooldownTracker();
        UUID player = UUID.randomUUID();
        assertTrue(tracker.allowAndRecord(player, 1000L, 0));
        assertTrue(tracker.allowAndRecord(player, 1001L, 0));
    }

    @Test
    void positive_cooldown_blocks_repeat_deaths_until_elapsed() {
        var tracker = new DeathCooldownTracker();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        assertTrue(tracker.allowAndRecord(alice, 10_000L, 30));
        assertFalse(tracker.allowAndRecord(alice, 25_000L, 30));
        assertTrue(tracker.allowAndRecord(bob, 25_000L, 30)); // independent victim
        assertTrue(tracker.allowAndRecord(alice, 40_000L, 30)); // 30s elapsed
    }
}
