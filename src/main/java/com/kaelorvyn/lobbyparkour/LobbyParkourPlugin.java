package com.kaelorvyn.lobbyparkour;

import com.earth2me.essentials.spawn.IEssentialsSpawn;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LobbyParkourPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String ESSENTIALS_SPAWN = "EssentialsSpawn";
    private static final Set<String> DOUBLE_JUMP_LABELS = Set.of("doublejumpz", "djz", "djump");

    private final ParkourState state = new ParkourState();
    private DoubleJumpController doubleJump;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        doubleJump = new DoubleJumpController(this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("kback").setExecutor(this);
        getCommand("kback").setTabCompleter(this);

        state.clearAll();
        Bukkit.getScheduler().runTask(this, () -> doubleJump.restoreAll(Bukkit.getOnlinePlayers()));
        getLogger().info("LobbyParkour 已启用：绿宝石存档、红石成功、/kback 回档。");
    }

    @Override
    public void onDisable() {
        if (doubleJump != null) {
            doubleJump.restoreAll(Bukkit.getOnlinePlayers());
        }
        state.clearAll();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getBlock().equals(to.getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        if (!isLobbyWorld(player.getWorld()) || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Material under = to.getBlock().getRelative(BlockFace.DOWN).getType();
        if (under == Material.EMERALD_BLOCK) {
            recordCheckpoint(player, to.getBlock().getRelative(BlockFace.DOWN));
        } else if (under == Material.REDSTONE_BLOCK) {
            complete(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!isLobbyWorld(player.getWorld())) {
            return;
        }
        Location spawn = lobbySpawn(player.getWorld());
        event.setRespawnLocation(spawn);
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleport(spawn.clone());
            player.setFallDistance(0.0f);
            doubleJump.enable(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        state.clearCheckpoint(player.getUniqueId());
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                doubleJump.enable(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        state.clearCheckpoint(event.getPlayer().getUniqueId());
        doubleJump.forget(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDoubleJumpCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!doubleJump.isLocked(player.getUniqueId())) {
            return;
        }

        String[] parts = event.getMessage().substring(1).trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);
        int separator = label.indexOf(':');
        if (separator >= 0) {
            label = label.substring(separator + 1);
        }
        if (!DOUBLE_JUMP_LABELS.contains(label)) {
            return;
        }

        String argument = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";
        if (argument.isEmpty() || argument.equals("on")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "当前在存档点状态，超级跳不可用。");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用 /kback。");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("exit")) {
            state.clearCheckpoint(player.getUniqueId());
            doubleJump.enable(player);
            player.sendMessage(message("reset", "&e已清除存档点，超级跳已恢复。"));
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.YELLOW + "用法：/kback 或 /kback exit");
            return true;
        }

        Location checkpoint = state.checkpoint(player.getUniqueId());
        if (checkpoint == null || checkpoint.getWorld() == null) {
            player.sendMessage(message("checkpoint-missing", "&c你还没有记录过存档点。"));
            return true;
        }

        player.teleport(checkpoint);
        player.setFallDistance(0.0f);
        doubleJump.disable(player);
        player.sendMessage(message("returned", "&e已返回最后一个存档点，超级跳已禁用。"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return "exit".startsWith(partial) ? List.of("exit") : List.of();
        }
        return List.of();
    }

    private void recordCheckpoint(Player player, org.bukkit.block.Block block) {
        Location checkpoint = block.getLocation().add(0.5, 1.0, 0.5);
        Location current = player.getLocation();
        checkpoint.setYaw(current.getYaw());
        checkpoint.setPitch(current.getPitch());
        state.setCheckpoint(player.getUniqueId(), checkpoint);
        doubleJump.disable(player);
        player.sendActionBar(message("checkpoint", "&a已记录存档点，超级跳已禁用。"));
    }

    private void complete(Player player) {
        state.clearCheckpoint(player.getUniqueId());
        Location spawn = lobbySpawn(player.getWorld());
        player.teleport(spawn);
        launchFirework(spawn);
        player.playSound(spawn, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendTitle(ChatColor.GREEN + "VICTORY", "", 5, 20, 5);
        doubleJump.enable(player);
    }

    private Location lobbySpawn(World world) {
        var plugin = getServer().getPluginManager().getPlugin(ESSENTIALS_SPAWN);
        if (plugin instanceof IEssentialsSpawn essentialsSpawn) {
            Location spawn = essentialsSpawn.getSpawn("default");
            if (spawn != null) {
                return spawn.clone();
            }
        }
        playerFallbackLog();
        return world.getSpawnLocation().clone();
    }

    private void playerFallbackLog() {
        if (!getConfig().getBoolean("runtime.spawn-fallback-logged", false)) {
            getConfig().set("runtime.spawn-fallback-logged", true);
            saveConfig();
            getLogger().warning(message("spawn-fallback", "&c大厅出生点读取失败，已使用世界出生点。"));
        }
    }

    private boolean isLobbyWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(getConfig().getString("world", "world"));
    }

    private String message(String path, String fallback) {
        String value = getConfig().getString("messages." + path, fallback);
        return ChatColor.translateAlternateColorCodes('&', value == null ? fallback : value);
    }

    private void launchFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        int first = ThreadLocalRandom.current().nextInt(1, 17);
        int second = ThreadLocalRandom.current().nextInt(1, 17);
        FireworkEffect effect = FireworkEffect.builder()
                .flicker(true)
                .withColor(fireworkColor(first))
                .withFade(fireworkColor(second))
                .with(FireworkEffect.Type.STAR)
                .trail(true)
                .build();
        meta.addEffect(effect);
        meta.setPower(0);
        firework.setFireworkMeta(meta);
    }

    private static Color fireworkColor(int value) {
        return switch (value) {
            case 2 -> Color.BLACK;
            case 3 -> Color.BLUE;
            case 4 -> Color.FUCHSIA;
            case 5 -> Color.GRAY;
            case 6 -> Color.GREEN;
            case 7 -> Color.LIME;
            case 8 -> Color.MAROON;
            case 9 -> Color.NAVY;
            case 10 -> Color.OLIVE;
            case 11 -> Color.ORANGE;
            case 12 -> Color.PURPLE;
            case 13 -> Color.RED;
            case 14 -> Color.SILVER;
            case 15 -> Color.TEAL;
            case 16 -> Color.WHITE;
            default -> Color.AQUA;
        };
    }
}
