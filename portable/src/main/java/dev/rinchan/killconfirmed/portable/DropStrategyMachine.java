package dev.rinchan.killconfirmed.portable;

public final class DropStrategyMachine {
    private DropStrategyMachine() {}

    public static DropTransition onDeath(DropStrategy strategy, PendingState current) {
        if (current == PendingState.PENDING) return new DropTransition(DropAction.NOOP, current);
        return strategy == DropStrategy.AT_DEATH_POSITION
                ? new DropTransition(DropAction.DROP_AT_DEATH, PendingState.EMPTY)
                : new DropTransition(DropAction.STORE_PENDING, PendingState.PENDING);
    }

    public static DropTransition onRespawn(PendingState current, boolean inventoryAccepted) {
        if (current == PendingState.EMPTY) return new DropTransition(DropAction.NOOP, current);
        return new DropTransition(inventoryAccepted ? DropAction.INSERT_AT_RESPAWN : DropAction.DROP_AT_RESPAWN,
                PendingState.EMPTY);
    }

    public static PendingState onLogout(PendingState current) { return current; }
}
