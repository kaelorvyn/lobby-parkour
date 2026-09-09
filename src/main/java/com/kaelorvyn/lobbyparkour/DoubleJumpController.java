package com.kaelorvyn.lobbyparkour;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class DoubleJumpController {
    private static final String USE_PERMISSION = "doublejumpz.use";

    private final JavaPlugin owner;
    private final Object doubleJumpPlugin;
    private final Field enabledPlayersField;
    private final Map<UUID, PermissionAttachment> deniedPermissions = new HashMap<>();
    private final Set<UUID> lockedPlayers = new HashSet<>();

    DoubleJumpController(JavaPlugin owner) {
        this.owner = owner;
        Plugin plugin = owner.getServer().getPluginManager().getPlugin("DoubleJumpZ");
        if (plugin == null) {
            throw new IllegalStateException("DoubleJumpZ is not loaded");
        }
        this.doubleJumpPlugin = plugin;
        this.enabledPlayersField = findEnabledPlayersField(plugin);
    }

    void disable(Player player) {
        UUID id = player.getUniqueId();
        lockedPlayers.add(id);

        if (player.hasPermission(USE_PERMISSION) && !deniedPermissions.containsKey(id)) {
            PermissionAttachment attachment = player.addAttachment(owner);
            attachment.setPermission(USE_PERMISSION, false);
            deniedPermissions.put(id, attachment);
        }

        Set<UUID> enabledPlayers = enabledPlayers();
        if (enabledPlayers != null) {
            enabledPlayers.remove(id);
        }
        player.setAllowFlight(false);
    }

    void enable(Player player) {
        UUID id = player.getUniqueId();
        lockedPlayers.remove(id);

        PermissionAttachment attachment = deniedPermissions.remove(id);
        if (attachment != null) {
            player.removeAttachment(attachment);
        }

        if (!player.hasPermission(USE_PERMISSION)) {
            player.setAllowFlight(false);
            return;
        }

        Set<UUID> enabledPlayers = enabledPlayers();
        if (enabledPlayers != null) {
            enabledPlayers.add(id);
        }
        player.setAllowFlight(true);
    }

    boolean isLocked(UUID playerId) {
        return lockedPlayers.contains(playerId);
    }

    void forget(Player player) {
        UUID id = player.getUniqueId();
        lockedPlayers.remove(id);
        PermissionAttachment attachment = deniedPermissions.remove(id);
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    void restoreAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            enable(player);
        }
    }

    private Set<UUID> enabledPlayers() {
        try {
            @SuppressWarnings("unchecked")
            Set<UUID> value = (Set<UUID>) enabledPlayersField.get(doubleJumpPlugin);
            return value;
        } catch (IllegalAccessException error) {
            owner.getLogger().warning("读取 DoubleJumpZ 状态失败: " + error.getMessage());
            return null;
        }
    }

    private static Field findEnabledPlayersField(Plugin plugin) {
        try {
            Field field = plugin.getClass().getDeclaredField("doubleJumpEnabled");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("当前 DoubleJumpZ 版本没有 doubleJumpEnabled 状态字段", error);
        }
    }
}
