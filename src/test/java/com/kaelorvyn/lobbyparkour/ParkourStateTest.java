package com.kaelorvyn.lobbyparkour;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ParkourStateTest {
    @Test
    void keepsOnlyThePlayersLastCheckpoint() {
        ParkourState state = new ParkourState();
        UUID player = UUID.randomUUID();
        Location first = new Location(null, 1.5, 65, 2.5);
        Location second = new Location(null, 8.5, 70, 4.5);

        state.setCheckpoint(player, first);
        state.setCheckpoint(player, second);

        assertEquals(second.getX(), state.checkpoint(player).getX());
        assertEquals(second.getY(), state.checkpoint(player).getY());
        assertEquals(second.getZ(), state.checkpoint(player).getZ());
    }

    @Test
    void checkpointSurvivesRetryUntilExplicitlyCleared() {
        ParkourState state = new ParkourState();
        UUID player = UUID.randomUUID();
        state.setCheckpoint(player, new Location(null, 1.5, 65, 2.5));

        assertNotNull(state.checkpoint(player));
        state.clearCheckpoint(player);
        assertNull(state.checkpoint(player));
    }
}
