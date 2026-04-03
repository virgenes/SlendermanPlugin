package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.compat.VersionCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FlashlightManager {

    private final Arena arena;
    
    // UUID -> Remaining Battery in Ticks (syncs to item on disable/drop)
    private final Map<UUID, Integer> activeFlashlights = new ConcurrentHashMap<>();
    
    // Used for smooth inertia camera matching
    private final Map<UUID, Vector> lastDirections = new ConcurrentHashMap<>();
    
    // Stores pre-calculated particle locations to spawn on sync thread
    private final Map<UUID, List<Location>> currentParticleDraws = new ConcurrentHashMap<>();

    private BukkitTask mainSyncTask;
    private BukkitTask asyncCalculationTask;
    
    // Config values
    private Material itemMaterial;
    private Component itemName;
    private Material brokenMaterial;
    private int maxTicks;
    private int drainRate;
    private int lowPercent;
    
    private double hAngle;
    private double vAngle;
    private double maxDist;
    private double ringSpacing;
    private int customUpdateInterval;
    
    private boolean useDarkness;
    private boolean useInertia;
    private boolean lowBatteryFlicker;
    
    // Sounds
    private String soundOn;
    private float soundOnPitch;
    private String soundOff;
    private float soundOffPitch;
    private String soundAmbient;
    private float soundAmbientVol;

    // Keys
    public static final NamespacedKey BATTERY_KEY = new NamespacedKey(SlenderMain.getInstance(), "flashlight_battery");

    public FlashlightManager(Arena arena) {
        this.arena = arena;
        loadConfig();
    }

    private void loadConfig() {
        YamlConfiguration config = Config.getConfiguration();
        try {
            itemMaterial = Material.valueOf(config.getString("Flashlight.Item.Material", "BLAZE_ROD"));
        } catch(Exception e) { itemMaterial = Material.BLAZE_ROD; }
        
        itemName = ColourUtil.colorizeToComponent(config.getString("Flashlight.Item.Name", "&6Linterna de Mano"));
        
        try {
            brokenMaterial = Material.valueOf(config.getString("Flashlight.Item.Broken-Material", "STICK"));
        } catch(Exception e) { brokenMaterial = Material.STICK; }

        maxTicks = config.getInt("Flashlight.Charge.Max-Ticks", 600);
        drainRate = config.getInt("Flashlight.Charge.Drain-Rate", 1);
        lowPercent = config.getInt("Flashlight.Charge.Low-Percent", 20);

        hAngle = config.getDouble("Flashlight.Light-Cone.Horizontal-Angle", 55.0);
        vAngle = config.getDouble("Flashlight.Light-Cone.Vertical-Angle", 22.0);
        maxDist = config.getDouble("Flashlight.Light-Cone.Max-Distance", 12.0);
        ringSpacing = config.getDouble("Flashlight.Light-Cone.Ring-Spacing", 1.5);
        customUpdateInterval = config.getInt("Flashlight.Light-Cone.Update-Interval-Ticks", 2);

        useDarkness = config.getBoolean("Flashlight.Visual.Darkness-Effect", true);
        useInertia = config.getBoolean("Flashlight.Visual.Inertia", true);
        lowBatteryFlicker = config.getBoolean("Flashlight.Visual.Low-Battery-Flicker", true);

        soundOn = config.getString("Flashlight.Sounds.On", "block.anvil.land");
        soundOnPitch = (float) config.getDouble("Flashlight.Sounds.On-Pitch", 0.8);
        soundOff = config.getString("Flashlight.Sounds.Off", "block.anvil.land");
        soundOffPitch = (float) config.getDouble("Flashlight.Sounds.Off-Pitch", 1.2);
        soundAmbient = config.getString("Flashlight.Sounds.Ambient", "block.beacon.ambient");
        soundAmbientVol = (float) config.getDouble("Flashlight.Sounds.Ambient-Volume", 0.3);
    }

    public void start() {
        // Asynchronous calculation for the bounding box of the cone
        asyncCalculationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(SlenderMain.getInstance(), () -> {
            for (UUID uuid : activeFlashlights.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                calculateCone(player);
            }
        }, 0L, customUpdateInterval);

        // Sync execution for applying effects, glowing, and particle rendering
        mainSyncTask = Bukkit.getScheduler().runTaskTimer(SlenderMain.getInstance(), () -> {
            Set<UUID> keys = new HashSet<>(activeFlashlights.keySet());
            for (UUID uuid : keys) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    toggle(player);
                    continue;
                }
                
                int battery = activeFlashlights.get(uuid) - drainRate;
                if (battery <= 0) {
                    battery = 0;
                    activeFlashlights.put(uuid, battery);
                    toggle(player);
                    breakFlashlight(player);
                    continue;
                }
                activeFlashlights.put(uuid, battery);

                // Play ambient noise
                if (Math.random() < 0.05) {
                    player.playSound(player.getLocation(), soundAmbient, soundAmbientVol, 1.0f);
                }

                if (useDarkness) {
                    VersionCompat.applyDarkness(player, 35, 0); // Keep Darkness up
                }
                
                // CRITICAL: Particles do not emit light! We must use Night Vision 
                // in combination with Darkness so blocks actually become visible.
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0, false, false, false));

                // Render Particles (Max 20 logic is internally smoothed)
                drawParticles(player, battery);
            }
            
            // Reapply Darkness to those who are off, if useDarkness is true and they are a survivor
            if (useDarkness) {
                for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
                    if (entry.getValue() == Role.SURVIVOR && entry.getKey().isOnline()) {
                        if (!activeFlashlights.containsKey(entry.getKey().getUniqueId())) {
                            VersionCompat.applyDarkness(entry.getKey(), 45, 1);
                        }
                    }
                }
            }

        }, 0L, 1L);
    }

    public void stop() {
        if (asyncCalculationTask != null) asyncCalculationTask.cancel();
        if (mainSyncTask != null) mainSyncTask.cancel();
        
        Set<UUID> keys = new HashSet<>(activeFlashlights.keySet());
        for (UUID uuid : keys) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                toggle(player); // Syncs battery
                if (useDarkness) {
                    VersionCompat.removeDarkness(player);
                }
            }
        }
        activeFlashlights.clear();
        currentParticleDraws.clear();
        lastDirections.clear();
    }

    public void giveFlashlight(Player player, int slot) {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(itemName);
            List<Component> lore = new ArrayList<>();
            lore.add(ColourUtil.colorizeToComponent("&7Right-click to toggle."));
            lore.add(ColourUtil.colorizeToComponent("&7Uses battery while active."));
            lore.add(ColourUtil.colorizeToComponent("&eBattery: " + maxTicks + " / " + maxTicks));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(BATTERY_KEY, PersistentDataType.INTEGER, maxTicks);
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(slot, item);
    }

    public void toggle(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        
        if (activeFlashlights.containsKey(uuid)) {
            // TURN OFF
            int remaining = activeFlashlights.remove(uuid);
            currentParticleDraws.remove(uuid);
            lastDirections.remove(uuid);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.playSound(player.getLocation(), soundOff, 1f, soundOffPitch);
            
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == itemMaterial) {
                updateItemBattery(item, remaining);
            }
        } else {
            // TURN ON
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == itemMaterial && item.hasItemMeta()) {
                int battery = item.getItemMeta().getPersistentDataContainer().getOrDefault(BATTERY_KEY, PersistentDataType.INTEGER, maxTicks);
                if (battery > 0) {
                    activeFlashlights.put(uuid, battery);
                    lastDirections.put(uuid, player.getEyeLocation().getDirection());
                    player.playSound(player.getLocation(), soundOn, 1f, soundOnPitch);
                }
            }
        }
    }

    public boolean isActive(Player player) {
        return activeFlashlights.containsKey(player.getUniqueId());
    }

    private void updateItemBattery(ItemStack item, int battery) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(BATTERY_KEY, PersistentDataType.INTEGER, battery);
            List<Component> lore = new ArrayList<>();
            lore.add(ColourUtil.colorizeToComponent("&7Right-click to toggle."));
            lore.add(ColourUtil.colorizeToComponent("&7Uses battery while active."));
            lore.add(ColourUtil.colorizeToComponent("&eBattery: " + battery + " / " + maxTicks));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
    }

    private void breakFlashlight(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == itemMaterial) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            item.setType(brokenMaterial);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ColourUtil.colorizeToComponent("&cLinterna Rota"));
                meta.lore(Collections.singletonList(ColourUtil.colorizeToComponent("&7Out of battery.")));
                item.setItemMeta(meta);
            }
        }
    }

    private void calculateCone(Player player) {
        UUID uuid = player.getUniqueId();
        Location eyeLoc = player.getEyeLocation();
        Vector currentDir = eyeLoc.getDirection();
        
        Vector direction;
        if (useInertia) {
            Vector last = lastDirections.getOrDefault(uuid, currentDir);
            // Lerp direction for weight
            direction = last.clone().multiply(0.6).add(currentDir.clone().multiply(0.4)).normalize();
            lastDirections.put(uuid, direction);
        } else {
            direction = currentDir;
        }

        List<Location> validParts = new ArrayList<>();
        
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        if (right.lengthSquared() == 0) right = new Vector(1, 0, 0); // edge case looking straight down
        Vector up = right.clone().crossProduct(direction).normalize();

        double tanH = Math.tan(Math.toRadians(hAngle / 2.0));
        double tanV = Math.tan(Math.toRadians(vAngle / 2.0));

        // Use thread-safe raytracing pseudo-sync approach if we want to avoid async restrictions
        // actually Bukkit world.rayTraceBlocks is mostly read-only thread-safe in chunk-loaded areas, but to be 100% compliant,
        // we shouldn't raytrace fully async. We will just compute the geometric points here, and raytrace them sync, 
        // OR we can just ignore blocks and just draw rings (which might go through walls).
        // Since we want performance: compute the offset vectors here.
        
        for (double d = 2.0; d <= maxDist; d += ringSpacing) {
            double rX = d * tanH;
            double rY = d * tanV;
            
            // 4 edge points per ring
            Vector[] offsets = new Vector[] {
                    right.clone().multiply(rX),
                    right.clone().multiply(-rX),
                    up.clone().multiply(rY),
                    up.clone().multiply(-rY)
            };
            
            for (Vector off : offsets) {
                Vector target = direction.clone().multiply(d).add(off);
                validParts.add(eyeLoc.clone().add(target));
            }
        }
        
        currentParticleDraws.put(uuid, validParts);
    }

    private void drawParticles(Player player, int battery) {
        List<Location> locs = currentParticleDraws.get(player.getUniqueId());
        if (locs == null || locs.isEmpty()) return;

        double percent = (double) battery / maxTicks * 100.0;
        
        // Flicker effect
        boolean drawThisTick = true;
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 230, 200), 0.5f);
        
        if (lowBatteryFlicker) {
            if (percent <= lowPercent) {
                if (Math.random() < 0.3) drawThisTick = false; // Erratic
                dust = new Particle.DustOptions(Color.fromRGB(200, 100, 50), 0.4f);
            } else if (percent <= 25) {
                if (Math.random() < 0.1) drawThisTick = false; // Soft flicker
            }
        }

        if (!drawThisTick) return;

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        
        // Glow effect (Entities in Cone)
        // Check entities around player directly using their bounding box since we're in the sync thread
        Collection<Entity> near = world.getNearbyEntities(eye, maxDist, maxDist, maxDist, e -> e instanceof LivingEntity && e != player);
        for (Entity e : near) {
            Vector toEntity = e.getLocation().toVector().subtract(eye.toVector());
            double dist = toEntity.length();
            if (dist > maxDist || dist == 0) continue;
            
            toEntity.normalize();
            double angle = Math.toDegrees(eye.getDirection().angle(toEntity));
            if (angle <= (hAngle / 2.0) + 5) {
                LivingEntity le = (LivingEntity) e;
                le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10, 0, false, false));
            }
        }

        // Limit particles to avoid rendering too many
        int rendered = 0;
        int targetMax = 20;
        
        for (Location loc : locs) {
            if (rendered >= targetMax) break;
            
            // Sync rayTrace for block occlusion is safe here
            RayTraceResult res = world.rayTraceBlocks(eye, loc.toVector().subtract(eye.toVector()).normalize(), eye.distance(loc), FluidCollisionMode.NEVER, true);
            if (res != null && res.getHitBlock() != null) {
                continue; // Blocked by wall
            }

            Particle dustParticle;
            try {
                dustParticle = Particle.valueOf("DUST");
            } catch (IllegalArgumentException ex) {
                dustParticle = Particle.valueOf("REDSTONE");
            }
            world.spawnParticle(dustParticle, loc, 1, 0.1, 0.1, 0.1, 0.0, dust);
            rendered++;
        }
    }
}
