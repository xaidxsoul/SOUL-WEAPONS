package me.soulweapons;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public final class SoulWeapons extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey weaponKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        weaponKey = new NamespacedKey(this, "soul_weapon");
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("soulweapons")).setExecutor(this);
        getLogger().info("SoulWeapons enabled!");
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private ItemStack createWeapon(String id) {
        Material material;
        String name;
        List<String> lore;
        switch (id.toLowerCase(Locale.ROOT)) {
            case "soulblade" -> { material = Material.NETHERITE_SWORD; name = "&5&lSoul Blade"; lore = List.of("&7A blade fueled by lost souls.", "&dAbility: Soul Drain", "&8Right-click to dash forward."); }
            case "stormaxe" -> { material = Material.NETHERITE_AXE; name = "&b&lStorm Axe"; lore = List.of("&7Crack the sky with every strike.", "&bAbility: Thunder Call", "&8Right-click to call lightning."); }
            case "inferno" -> { material = Material.BLAZE_ROD; name = "&c&lInferno"; lore = List.of("&7A weapon burning with ancient fire.", "&cAbility: Flame Burst", "&8Right-click to ignite nearby enemies."); }
            case "reaper" -> { material = Material.NETHERITE_HOE; name = "&4&lReaper"; lore = List.of("&7Steal vitality from your enemies.", "&4Ability: Lifesteal", "&8Hits restore a little health."); }
            case "titanhammer" -> { material = Material.NETHERITE_PICKAXE; name = "&6&lTitan Hammer"; lore = List.of("&7A crushing weapon of the old titans.", "&6Ability: Ground Slam", "&8Right-click to blast nearby enemies."); }
            default -> { return null; }
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(this::color).toList());
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, id.toLowerCase(Locale.ROOT));
        item.setItemMeta(meta);
        return item;
    }

    private String weaponId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
    }

    @EventHandler public void onUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        String id = weaponId(player.getInventory().getItemInMainHand());
        if (id == null) return;
        event.setCancelled(true);
        if (!ready(player)) return;
        switch (id) {
            case "soulblade" -> {
                Vector direction = player.getLocation().getDirection().normalize().multiply(getConfig().getDouble("settings.dash-strength", 1.7));
                direction.setY(Math.max(0.25, direction.getY()));
                player.setVelocity(direction);
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 30, .5, .5, .5, .04);
                player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.4f);
            }
            case "stormaxe" -> {
                double radius = getConfig().getDouble("settings.lightning-radius", 6.0);
                for (Entity entity : player.getNearbyEntities(radius, radius, radius))
                    if (entity instanceof LivingEntity target && !(target instanceof Player p && p.getUniqueId().equals(player.getUniqueId())))
                        player.getWorld().strikeLightningEffect(target.getLocation());
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0,1,0), 45, 2, 1, 2, .1);
                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
            }
            case "inferno" -> {
                for (Entity entity : player.getNearbyEntities(4, 3, 4))
                    if (entity instanceof LivingEntity target && !(target instanceof Player p && p.getUniqueId().equals(player.getUniqueId()))) {
                        target.setFireTicks(100);
                        target.damage(3.0, player);
                    }
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0,1,0), 60, 2, 1, 2, .04);
                player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, .8f);
            }
            case "titanhammer" -> {
                for (Entity entity : player.getNearbyEntities(4, 2, 4))
                    if (entity instanceof LivingEntity target && !(target instanceof Player p && p.getUniqueId().equals(player.getUniqueId()))) {
                        target.damage(6.0, player);
                        Vector knock = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2);
                        knock.setY(.65); target.setVelocity(knock);
                    }
                player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 45, 2, .2, 2, Material.DEEPSLATE.createBlockData());
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, .8f, 1.3f);
            }
            default -> { return; }
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("settings.ability-cooldown-seconds", 8) * 1000L);
        player.sendMessage(color("&8[&5SoulWeapons&8] &dAbility activated!"));
    }

    @EventHandler public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        String id = weaponId(player.getInventory().getItemInMainHand());
        if ("reaper".equals(id)) {
            double heal = getConfig().getDouble("settings.lifesteal-heal", 2.0);
            Attribute attribute = Attribute.MAX_HEALTH;
            double max = Objects.requireNonNull(player.getAttribute(attribute)).getValue();
            player.setHealth(Math.min(max, player.getHealth() + heal));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, .3, .3, .3);
        }
    }

    private boolean ready(Player player) {
        long until = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (until > System.currentTimeMillis()) {
            player.sendMessage(color("&8[&5SoulWeapons&8] &cAbility cooldown: " + ((until-System.currentTimeMillis()+999)/1000) + "s"));
            return false;
        }
        return true;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&5&lSoulWeapons &7Weapons: &dSoulBlade, StormAxe, Inferno, Reaper, TitanHammer"));
            return true;
        }
        if (!sender.hasPermission("soulweapons.admin")) {
            sender.sendMessage(color(getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig(); sender.sendMessage(color("&aSoulWeapons config reloaded.")); return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) { sender.sendMessage(color("&cUsage: /sw give <player> <weapon>")); return true; }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(color("&cPlayer is not online.")); return true; }
            ItemStack item = createWeapon(args[2]);
            if (item == null) { sender.sendMessage(color("&cUnknown weapon. Use /sw list.")); return true; }
            target.getInventory().addItem(item);
            sender.sendMessage(color("&aGave " + args[2] + " to " + target.getName() + "."));
            return true;
        }
        sender.sendMessage(color("&cUsage: /sw <give|list|reload>"));
        return true;
    }
}
