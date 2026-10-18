package com.example.duels;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class DuelsPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final HashMap<UUID, Location> savedLocations = new HashMap<>();
    private final Map<Location, BlockState> originalBlockStates = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("duels") != null) getCommand("duels").setExecutor(this);
        if (getCommand("kit") != null) getCommand("kit").setExecutor(this);
        if (getCommand("tpback") != null) getCommand("tpback").setExecutor(this);
        if (getCommand("tp") != null) getCommand("tp").setExecutor(this);

        getConfig().addDefault("arena.x", 0.5);
        getConfig().addDefault("arena.y", 100.0);
        getConfig().addDefault("arena.z", 0.5);
        getConfig().addDefault("arena.world", "world");
        getConfig().options().copyDefaults(true);
        saveConfig();

        getLogger().info("Duels Plugin Enabled Successfully!");
    }

    @Override
    public void onDisable() {
        resetArenaBlocks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use duel commands.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("duels")) {
            savedLocations.put(player.getUniqueId(), player.getLocation());
            teleportToArena(player);
            player.sendMessage(ChatColor.GREEN + "Teleported to the Duel Arena! Opening kit menu...");
            openKitMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("kit")) {
            openKitMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpback") || 
           (command.getName().equalsIgnoreCase("tp") && args.length > 0 && args[0].equalsIgnoreCase("back"))) {

            if (savedLocations.containsKey(player.getUniqueId())) {
                Location backLoc = savedLocations.remove(player.getUniqueId());
                player.teleport(backLoc);
                player.sendMessage(ChatColor.GREEN + "Teleported back to your original location!");
            } else {
                player.sendMessage(ChatColor.RED + "You don't have a saved location to return to.");
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        originalBlockStates.putIfAbsent(block.getLocation(), block.getState());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (event.getBlockReplacedState() != null) {
            originalBlockStates.putIfAbsent(block.getLocation(), event.getBlockReplacedState());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            originalBlockStates.putIfAbsent(block.getLocation(), block.getState());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (savedLocations.containsKey(player.getUniqueId())) {
            savedLocations.remove(player.getUniqueId());
            resetArenaBlocks();
            Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " logged out during a duel. Arena reset!");
        }
    }

    private void resetArenaBlocks() {
        if (originalBlockStates.isEmpty()) return;

        for (BlockState state : originalBlockStates.values()) {
            state.update(true, false);
        }
        originalBlockStates.clear();
    }

    public void openKitMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_RED + "Select Your Kit");

        gui.setItem(0, createGuiItem(Material.END_CRYSTAL, ChatColor.LIGHT_PURPLE + "Crystal Kit", "Crystals, Obsidian & Respawn Anchors"));
        gui.setItem(2, createGuiItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "Sword Kit", "Diamond Sword & Armor"));
        gui.setItem(4, createGuiItem(Material.TNT_MINECART, ChatColor.RED + "TNT Minecart Kit", "Minecarts, Rails & Flame Bow"));
        gui.setItem(6, createGuiItem(Material.NETHERITE_AXE, ChatColor.DARK_AQUA + "Axe Kit", "Shield & Netherite Axe PvP"));
        gui.setItem(8, createGuiItem(Material.NETHERITE_CHESTPLATE, ChatColor.DARK_PURPLE + "Netherite OP Kit", "Full Netherite Gear & Potions"));

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack
