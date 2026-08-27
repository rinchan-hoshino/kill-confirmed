package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DropStrategyMachineTest {
    @Test
    void death_position_emits_once_and_never_creates_pending_state() {
        var first = DropStrategyMachine.onDeath(DropStrategy.AT_DEATH_POSITION, PendingState.EMPTY);
        assertEquals(DropAction.DROP_AT_DEATH, first.action());
        assertEquals(PendingState.EMPTY, first.next());
    }

    @Test
    void respawn_mode_persists_one_then_consumes_before_delivery() {
        var death = DropStrategyMachine.onDeath(DropStrategy.ON_RESPAWN_INVENTORY, PendingState.EMPTY);
        assertEquals(DropAction.STORE_PENDING, death.action());
        assertEquals(PendingState.PENDING, death.next());
        assertEquals(DropAction.NOOP, DropStrategyMachine.onDeath(
                DropStrategy.ON_RESPAWN_INVENTORY, death.next()).action());

        var accepted = DropStrategyMachine.onRespawn(death.next(), true);
        assertEquals(DropAction.INSERT_AT_RESPAWN, accepted.action());
        assertEquals(PendingState.EMPTY, accepted.next());
        assertEquals(DropAction.NOOP, DropStrategyMachine.onRespawn(accepted.next(), true).action());
    }

    @Test
    void full_inventory_drops_at_respawn_and_logout_keeps_pending() {
        assertEquals(PendingState.PENDING, DropStrategyMachine.onLogout(PendingState.PENDING));
        var result = DropStrategyMachine.onRespawn(PendingState.PENDING, false);
        assertEquals(DropAction.DROP_AT_RESPAWN, result.action());
        assertEquals(PendingState.EMPTY, result.next());
    }
}
