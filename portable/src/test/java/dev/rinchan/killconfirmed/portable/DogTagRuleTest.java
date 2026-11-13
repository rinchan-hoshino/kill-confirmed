package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DogTagRuleTest {
    @Test
    void every_death_creates_a_tag_when_player_kills_only_is_off() {
        assertTrue(DogTagRule.createsTag(false, false));
        assertTrue(DogTagRule.createsTag(false, true));
    }

    @Test
    void only_player_kills_create_a_tag_when_player_kills_only_is_on() {
        assertTrue(DogTagRule.createsTag(true, true));
        assertFalse(DogTagRule.createsTag(true, false));
    }
}
