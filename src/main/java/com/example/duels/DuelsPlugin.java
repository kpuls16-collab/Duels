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

    // Store players' original locations prior to duel
    private final HashMap<UUID, Location> savedLocations = new HashMap<>();

    // Track modified blocks to restore them on logout or reset
    private final Map<Location, BlockState> originalBlockStates = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Register command executors properly so all commands actually work
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

        // Command: /duels
        if (command.getName().equalsIgnoreCase("duels")) {
            savedLocations.put(player.getUniqueId(), player.getLocation());
            teleportToArena(player);
            player.sendMessage(ChatColor.GREEN + "Teleported to the Duel Arena! Opening kit menu...");
            openKitMenu(player);
            return true;
        }

        // Command: /kit
        if (command.getName().equalsIgnoreCase("kit")) {
            openKitMenu(player);
            return true;
        }

        // Command: /tpback OR /tp back
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

    // --- BLOCK TRACKING & AUTOMATIC ARENA RESET ---

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

    // --- GUI & KITS ---

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
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        // Strip color codes for cross-play safety (Geyser compatibility)
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.equals("Select Your Kit")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        player.getInventory().clear();

        switch (event.getSlot()) {
            case 0: // Crystal Kit
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
                player.getInventory().addItem(new ItemStack(Material.END_CRYSTAL, 64));
                player.getInventory().addItem(new ItemStack(Material.OBSIDIAN, 64));
                player.getInventory().addItem(new ItemStack(Material.RESPAWN_ANCHOR, 32));
                player.getInventory().addItem(new ItemStack(Material.GLOWSTONE, 64));
                player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
                
                player.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Selected Crystal Kit!");
                break;

            case 2: // Sword Kit
                player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
                
                player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                player.sendMessage(ChatColor.GOLD + "Selected Sword Kit!");
                break;

            case 4: // TNT Minecart Kit
                player.getInventory().addItem(new ItemStack(Material.TNT_MINECART, 32));
                player.getInventory().addItem(new ItemStack(Material.RAIL, 64));
                player.getInventory().addItem(new ItemStack(Material.FLINT_AND_STEEL));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
                
                player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                player.sendMessage(ChatColor.RED + "Selected TNT Minecart Kit!");
                break;

            case 6: // Axe Kit
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_AXE));
                player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                player.getInventory().addItem(new ItemStack(Material.CROSSBOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 12));
                
                player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                player.sendMessage(ChatColor.DARK_AQUA + "Selected Axe Kit!");
                break;

            case 8: // Netherite OP Kit
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
                player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 16));
                
                player.getInventory().addItem(createCustomSplashPotion(PotionEffectType.INSTANT_HEALTH, 1, 1));
                player.getInventory().addItem(createCustomSplashPotion(PotionEffectType.STRENGTH, 180 * 20, 1));
                player.getInventory().addItem(createCustomSplashPotion(PotionEffectType.SPEED, 180 * 20, 1));
                
                player.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                player.sendMessage(ChatColor.DARK_PURPLE + "Selected Netherite OP Kit!");
                break;
        }

        player.closeInventory();
    }

    private ItemStack createCustomSplashPotion(PotionEffectType type, int durationTicks, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
            potion.setItemMeta(meta);
        }
        return potion;
    }

    // --- WINNER LOGIC & ARENA RESET ---

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // If they died during an active duel session, clean up drops to prevent clutter
        if (savedLocations.containsKey(victim.getUniqueId())) {
            event.getDrops().clear();
        }

        // Automatically clean up arena blocks when a duel concludes via death
        resetArenaBlocks();

        if (killer != null) {
            killer.sendMessage(ChatColor.GOLD + "🎉 You won the duel! You get to keep your kit.");
            victim.sendMessage(ChatColor.RED + "You were defeated. Type /tp back to return.");
        }
    }

    private void teleportToArena(Player player) {
        String worldName = getConfig().getString("arena.world", "world");
        double x = getConfig().getDouble("arena.x");
        double y = getConfig().getDouble("arena.y");
        double z = getConfig().getDouble("arena.z");

        if (Bukkit.getWorld(worldName) != null) {
            Location arenaLoc = new Location(Bukkit.getWorld(worldName), x, y, z);
            player.teleport(arenaLoc);
        } else {
            player.sendMessage(ChatColor.RED + "Error: Arena world not found!");
        }
    }
                                    }
