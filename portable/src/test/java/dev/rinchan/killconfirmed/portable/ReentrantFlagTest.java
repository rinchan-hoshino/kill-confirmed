package dev.rinchan.killconfirmed.portable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReentrantFlagTest {
    @Test
    void nestedScopesRemainActiveUntilTheOutermostScopeCloses() {
        ReentrantFlag flag = new ReentrantFlag();
        assertFalse(flag.isSet());
        try (ReentrantFlag.Scope outer = flag.enter()) {
            assertTrue(flag.isSet());
            try (ReentrantFlag.Scope inner = flag.enter()) {
                assertTrue(flag.isSet());
            }
            assertTrue(flag.isSet());
        }
        assertFalse(flag.isSet());
    }

    @Test
    void tryWithResourcesClearsTheFlagAfterFailure() {
        ReentrantFlag flag = new ReentrantFlag();
        assertThrows(IllegalStateException.class, () -> {
            try (ReentrantFlag.Scope ignored = flag.enter()) {
                throw new IllegalStateException("boom");
            }
        });
        assertFalse(flag.isSet());
    }

    @Test
    void aScopeCanOnlyBeClosedOnce() {
        ReentrantFlag flag = new ReentrantFlag();
        ReentrantFlag.Scope scope = flag.enter();
        scope.close();
        assertThrows(IllegalStateException.class, scope::close);
        assertFalse(flag.isSet());
    }
}
