package dev.rinchan.killconfirmed.portable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-victim anti-farm cooldown tracker for dog tag drops. */
public final class DeathCooldownTracker {
    private final Map<UUID, Long> lastDropMillis = new ConcurrentHashMap<>();

    public boolean allowAndRecord(UUID victimId, long nowMillis, int cooldownSeconds) {
        Objects.requireNonNull(victimId, "victimId");
        if (cooldownSeconds <= 0) {
            lastDropMillis.put(victimId, nowMillis);
            return true;
        }
        long cooldownMs = cooldownSeconds * 1000L;
        Long previous = lastDropMillis.get(victimId);
        if (previous != null && (nowMillis - previous) < cooldownMs) {
            return false;
        }
        lastDropMillis.put(victimId, nowMillis);
        return true;
    }

    public void clear() {
        lastDropMillis.clear();
    }
}
