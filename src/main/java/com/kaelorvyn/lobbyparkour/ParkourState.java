package com.kaelorvyn.lobbyparkour;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class ParkourState {
    private final Map<UUID, Location> checkpoints = new HashMap<>();

    void setCheckpoint(UUID playerId, Location location) {
        checkpoints.put(playerId, location.clone());
    }

    Location checkpoint(UUID playerId) {
        Location location = checkpoints.get(playerId);
        return location == null ? null : location.clone();
    }

    void clearCheckpoint(UUID playerId) {
        checkpoints.remove(playerId);
    }

    void clearAll() {
        checkpoints.clear();
    }
}
