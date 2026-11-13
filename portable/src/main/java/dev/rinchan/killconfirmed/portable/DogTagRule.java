package dev.rinchan.killconfirmed.portable;

/** Decides whether a death creates a dog tag under the {@code player_kills_only} setting. */
public final class DogTagRule {
    private DogTagRule() {}

    /**
     * With {@code playerKillsOnly} off every death qualifies. With it on, only deaths caused by
     * another player do; the caller resolves projectiles to their shooter before asking.
     */
    public static boolean createsTag(boolean playerKillsOnly, boolean killedByOtherPlayer) {
        return !playerKillsOnly || killedByOtherPlayer;
    }
}
