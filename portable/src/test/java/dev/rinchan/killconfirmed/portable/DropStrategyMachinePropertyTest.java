package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DropStrategyMachinePropertyTest {
    private static final Machine PRODUCTION = new Machine() {
        @Override
        public DropTransition onDeath(DropStrategy strategy, PendingState state) {
            return DropStrategyMachine.onDeath(strategy, state);
        }

        @Override
        public DropTransition onRespawn(PendingState state, boolean inventoryAccepted) {
            return DropStrategyMachine.onRespawn(state, inventoryAccepted);
        }

        @Override
        public DropTransition onLogout(PendingState state) {
            return new DropTransition(DropAction.NOOP, DropStrategyMachine.onLogout(state));
        }
    };

    @Test
    void everySequenceUpToEightEventsMatchesTheSinglePendingReferenceModel() {
        verifyAllSequences(PRODUCTION, 8);
    }

    @Test
    void negativeFixtureRejectsRespawnDeliveryThatDoesNotConsumePending() {
        Machine doesNotConsumePending = new Machine() {
            @Override
            public DropTransition onDeath(DropStrategy strategy, PendingState state) {
                return DropStrategyMachine.onDeath(strategy, state);
            }

            @Override
            public DropTransition onRespawn(PendingState state, boolean inventoryAccepted) {
                DropTransition actual = DropStrategyMachine.onRespawn(state, inventoryAccepted);
                return state == PendingState.PENDING
                    ? new DropTransition(actual.action(), PendingState.PENDING)
                    : actual;
            }

            @Override
            public DropTransition onLogout(PendingState state) {
                return new DropTransition(DropAction.NOOP, state);
            }
        };

        assertThrows(AssertionError.class, () -> verifyAllSequences(doesNotConsumePending, 2));
    }

    private static void verifyAllSequences(Machine machine, int maxDepth) {
        verifyFrom(machine, maxDepth, PendingState.EMPTY, PendingState.EMPTY, new ArrayList<>());
    }

    private static void verifyFrom(
        Machine machine,
        int remainingDepth,
        PendingState expectedState,
        PendingState actualState,
        List<Operation> sequence
    ) {
        if (remainingDepth == 0) {
            return;
        }
        for (Operation operation : Operation.values()) {
            sequence.add(operation);
            DropTransition expected = referenceTransition(operation, expectedState);
            DropTransition actual = operation.apply(machine, actualState);
            assertEquals(expected, actual, () -> "sequence=" + List.copyOf(sequence));
            verifyFrom(machine, remainingDepth - 1, expected.next(), actual.next(), sequence);
            sequence.remove(sequence.size() - 1);
        }
    }

    private static DropTransition referenceTransition(Operation operation, PendingState state) {
        return switch (operation) {
            case DEATH_AT_POSITION -> state == PendingState.PENDING
                ? new DropTransition(DropAction.NOOP, PendingState.PENDING)
                : new DropTransition(DropAction.DROP_AT_DEATH, PendingState.EMPTY);
            case DEATH_ON_RESPAWN -> state == PendingState.PENDING
                ? new DropTransition(DropAction.NOOP, PendingState.PENDING)
                : new DropTransition(DropAction.STORE_PENDING, PendingState.PENDING);
            case RESPAWN_ACCEPTED -> state == PendingState.EMPTY
                ? new DropTransition(DropAction.NOOP, PendingState.EMPTY)
                : new DropTransition(DropAction.INSERT_AT_RESPAWN, PendingState.EMPTY);
            case RESPAWN_FULL -> state == PendingState.EMPTY
                ? new DropTransition(DropAction.NOOP, PendingState.EMPTY)
                : new DropTransition(DropAction.DROP_AT_RESPAWN, PendingState.EMPTY);
            case LOGOUT -> new DropTransition(DropAction.NOOP, state);
        };
    }

    private interface Machine {
        DropTransition onDeath(DropStrategy strategy, PendingState state);

        DropTransition onRespawn(PendingState state, boolean inventoryAccepted);

        DropTransition onLogout(PendingState state);
    }

    private enum Operation {
        DEATH_AT_POSITION {
            @Override
            DropTransition apply(Machine machine, PendingState state) {
                return machine.onDeath(DropStrategy.AT_DEATH_POSITION, state);
            }
        },
        DEATH_ON_RESPAWN {
            @Override
            DropTransition apply(Machine machine, PendingState state) {
                return machine.onDeath(DropStrategy.ON_RESPAWN_INVENTORY, state);
            }
        },
        RESPAWN_ACCEPTED {
            @Override
            DropTransition apply(Machine machine, PendingState state) {
                return machine.onRespawn(state, true);
            }
        },
        RESPAWN_FULL {
            @Override
            DropTransition apply(Machine machine, PendingState state) {
                return machine.onRespawn(state, false);
            }
        },
        LOGOUT {
            @Override
            DropTransition apply(Machine machine, PendingState state) {
                return machine.onLogout(state);
            }
        };

        abstract DropTransition apply(Machine machine, PendingState state);
    }
}
