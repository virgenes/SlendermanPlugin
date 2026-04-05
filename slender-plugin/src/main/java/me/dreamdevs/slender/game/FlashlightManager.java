package me.dreamdevs.slender.game;

import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.Config;
import me.dreamdevs.slender.api.Setting;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.utils.ColourUtil;
import me.dreamdevs.slender.database.data.GamePlayer;
import me.dreamdevs.slender.compat.VersionCompat;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlashlightManager {

    private final Arena arena;
    
    // UUID -> Remaining Battery in Ticks (syncs to item on disable/drop)
    private final Map<UUID, Integer> activeFlashlights = new ConcurrentHashMap<>();
    
    // Used for smooth inertia camera matching
    private final Map<UUID, Vector> lastDirections = new ConcurrentHashMap<>();
    
    // Stores pre-calculated particle locations to spawn on sync thread
    private final Map<UUID, List<Location>> currentParticleDraws = new ConcurrentHashMap<>();

    // Track virtual light sources per player (List for path illumination)
    private final Map<UUID, List<Location>> lastLightLocations = new ConcurrentHashMap<>();

    // Track last known setting to detect changes while active
    private final Map<UUID, Boolean> lastRealisticSetting = new ConcurrentHashMap<>();

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

    public void loadConfig() {
        YamlConfiguration config = Config.getConfiguration();
        try {
            itemMaterial = Material.valueOf(config.getString("Flashlight.Item.Material", "BLAZE_ROD"));
        } catch(Exception e) { itemMaterial = Material.BLAZE_ROD; }
        
        itemName = ColourUtil.colorizeToComponent(config.getString("Flashlight.Item.Name", "&6Linterna de Mano"));
        
        try {
            brokenMaterial = Material.valueOf(config.getString("Flashlight.Item.Broken-Material", "STICK"));
        } catch(Exception e) { brokenMaterial = Material.STICK; }

        maxTicks = config.getInt("Flashlight.Charge.Max-Ticks", 2400);
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

                GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(player);
                if (gp == null || arena == null) continue;
                
                boolean flicker = gp.getSetting(Setting.DARKNESS_FLICKER) == null || (boolean) gp.getSetting(Setting.DARKNESS_FLICKER);
                boolean realistic = gp.getSetting(Setting.FLASHLIGHT_REALISTIC) == null || (boolean) gp.getSetting(Setting.FLASHLIGHT_REALISTIC);

                // Handle setting change while active
                if (lastRealisticSetting.containsKey(uuid) && lastRealisticSetting.get(uuid) != realistic) {
                    if (realistic) {
                        // Switched TO realistic: remove Night Vision
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    } else {
                        // Switched TO night vision: remove Light blocks
                        List<Location> last = lastLightLocations.remove(uuid);
                        if (last != null) {
                            for (Location loc : last) {
                                player.sendBlockChange(loc, loc.getBlock().getBlockData());
                            }
                        }
                    }
                }
                lastRealisticSetting.put(uuid, realistic);

                if (useDarkness) {
                    PotionEffectType darknessType = PotionEffectType.DARKNESS;
                    if (flicker) {
                        PotionEffect activeDarkness = player.getPotionEffect(darknessType != null ? darknessType : PotionEffectType.BLINDNESS);
                        // Re-apply only if missing or expiring to prevent visual packet flood (flicker)
                        if (activeDarkness == null || activeDarkness.getDuration() < 10) {
                            VersionCompat.applyDarkness(player, 35, 0);
                        }
                    } else {
                        // Flicker OFF: Use Atmospheric Fog instead of Darkness/Blindness
                        if (darknessType != null) player.removePotionEffect(darknessType);
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        
                        // Apply Slowness I for FOV narrowing (slight zoom/claustrophobia)
                        if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                        }
                        
                        // Spawn personal fog particles around the player
                        spawnAtmosphericFog(player);
                    }
                }
                
                if (realistic) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    // Render Particles and update virtual light
                    drawParticles(player, battery);
                    updateVirtualLight(player);
                } else {
                    // Night vision style: Infinite duration while holding to prevent "ugly" flickering
                    if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                    }
                    // Minimal particles to indicate it's on
                    if (Math.random() < 0.1) {
                         player.getWorld().spawnParticle(Particle.SMOKE, player.getEyeLocation().add(player.getEyeLocation().getDirection()), 1, 0, 0, 0, 0.01);
                    }
                }
            }
            
            // Reapply Darkness to those who are off, if useDarkness is true and they are a survivor
            if (useDarkness) {
                for (Map.Entry<Player, Role> entry : arena.getPlayers().entrySet()) {
                    Player p = entry.getKey();
                    if (entry.getValue() == Role.SURVIVOR && p.isOnline()) {
                        if (!activeFlashlights.containsKey(p.getUniqueId())) {
                            GamePlayer gp = SlenderMain.getInstance().getPlayerManager().getPlayer(p);
                            if (gp == null) continue;
                            boolean flicker = gp.getSetting(Setting.DARKNESS_FLICKER) == null || (boolean) gp.getSetting(Setting.DARKNESS_FLICKER);
                            
                            if (flicker) {
                                PotionEffect activeDarkness = p.getPotionEffect(PotionEffectType.DARKNESS != null ? PotionEffectType.DARKNESS : PotionEffectType.BLINDNESS);
                                if (activeDarkness == null || activeDarkness.getDuration() < 15) {
                                    VersionCompat.applyDarkness(p, 45, 1);
                                }
                            } else {
                                // Flicker OFF: Use Fog
                                PotionEffectType darknessType = PotionEffectType.DARKNESS;
                                if (darknessType != null) p.removePotionEffect(darknessType);
                                p.removePotionEffect(PotionEffectType.BLINDNESS);
                                
                                if (!p.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                                }
                                spawnAtmosphericFog(p);
                            }
                        }
                    }
                }
            }

        }, 0L, 1L);
    }

    private void updateVirtualLight(Player player) {
        UUID uuid = player.getUniqueId();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        
        // Raytrace to find beam length and hit face
        RayTraceResult result = player.getWorld().rayTraceBlocks(eye, dir, maxDist, FluidCollisionMode.NEVER, true);
        double endDist = (result != null && result.getHitPosition() != null) ? eye.distance(result.getHitPosition().toLocation(player.getWorld())) : maxDist;
        
        List<Location> newLocs = new ArrayList<>();
        // 1. Path Illumination (Light sources along the beam)
        for (double d = 1.6; d < endDist - 1.0; d += 3.8) {
            Location loc = eye.clone().add(dir.clone().multiply(d));
            if (loc.getBlock().isReplaceable()) {
                newLocs.add(loc);
            }
        }
        
        // 2. Impact Point Bloom (More realistic and expanded lighting)
        Block hitBlock = (result != null) ? result.getHitBlock() : null;
        BlockFace hitFace = (result != null) ? result.getHitBlockFace() : null;
        
        if (hitBlock != null && hitFace != null) {
            // Place main light in the air block adjacent to the hit face (fixes disappearing objects)
            Location center = hitBlock.getRelative(hitFace).getLocation().toCenterLocation();
            if (center.getBlock().isReplaceable()) {
                newLocs.add(center);
                
                // Add 4 more light sources around the impact point for "Bloom/Expanded" effect
                // Use perpendicular vectors to the hit face to spread out the light
                Vector hitVec = hitFace.getDirection();
                Vector planeX = (Math.abs(hitVec.getX()) > 0.5 || Math.abs(hitVec.getZ()) > 0.5) ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
                Vector planeY = hitVec.crossProduct(planeX).normalize();
                planeX = hitVec.crossProduct(planeY).normalize();
                
                // Smaller offsets (around 1 block) to create a denser, bigger glow
                double offset = 0.9;
                Location[] surrounds = {
                    center.clone().add(planeX.clone().multiply(offset)),
                    center.clone().add(planeX.clone().multiply(-offset)),
                    center.clone().add(planeY.clone().multiply(offset)),
                    center.clone().add(planeY.clone().multiply(-offset))
                };
                
                for (Location loc : surrounds) {
                    if (loc.getBlock().isReplaceable()) newLocs.add(loc);
                }
            }
        } else {
            // If no wall, just place a light point in the air at the end
            Location airEnd = eye.clone().add(dir.clone().multiply(maxDist));
            if (airEnd.getBlock().isReplaceable()) {
                newLocs.add(airEnd);
            }
        }

        List<Location> lastLocs = lastLightLocations.get(uuid);
        Location lastEnd = (lastLocs == null || lastLocs.isEmpty()) ? null : lastLocs.get(lastLocs.size() - 1);
        Location newEnd = newLocs.isEmpty() ? null : newLocs.get(newLocs.size() - 1);

        // Update if significantly moved or first run
        if (newEnd != null && (lastEnd == null || lastEnd.distanceSquared(newEnd) > 1.4)) {
            // Remove old lights
            if (lastLocs != null) {
                for (Location loc : lastLocs) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
            
            // Send new light block changes (Material.LIGHT level 15)
            try {
                org.bukkit.block.data.type.Light lightData = (org.bukkit.block.data.type.Light) Bukkit.createBlockData(Material.LIGHT);
                lightData.setLevel(15);
                
                for (Location loc : newLocs) {
                    player.sendBlockChange(loc, lightData);
                }
                lastLightLocations.put(uuid, newLocs);
            } catch (Exception ignored) {} 
        }
    }

    public void stop() {
        if (asyncCalculationTask != null) asyncCalculationTask.cancel();
        if (mainSyncTask != null) mainSyncTask.cancel();
        
        Set<UUID> keys = new HashSet<>(activeFlashlights.keySet());
        for (UUID uuid : keys) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                toggle(player); // Syncs battery and removes virtual light
                if (useDarkness) {
                    VersionCompat.removeDarkness(player);
                }
            }
        }
        activeFlashlights.clear();
        currentParticleDraws.clear();
        lastDirections.clear();
        lastLightLocations.clear();
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
            int battery = activeFlashlights.remove(uuid);
            currentParticleDraws.remove(uuid);
            lastDirections.remove(uuid);
            
            // Remove virtual light path
            List<Location> last = lastLightLocations.remove(uuid);
            if (last != null) {
                for (Location loc : last) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
            lastRealisticSetting.remove(uuid);

            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.playSound(player.getLocation(), soundOff, 1f, soundOffPitch);
            
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == itemMaterial) {
                updateItemBattery(item, battery);
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
        List<Location> ringLocs = currentParticleDraws.get(player.getUniqueId());
        
        double percent = (double) battery / maxTicks * 100.0;
        boolean drawThisTick = true;
        if (lowBatteryFlicker) {
            if (percent <= lowPercent) {
                if (Math.random() < 0.3) drawThisTick = false;
            } else if (percent <= 25) {
                if (Math.random() < 0.1) drawThisTick = false;
            }
        }

        if (!drawThisTick) return;

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        // 1. DENSE VOLUMETRIC BEAM (Ray of light)
        RayTraceResult trace = world.rayTraceBlocks(eye, dir, maxDist, FluidCollisionMode.NEVER, true);
        double endDist = (trace != null && trace.getHitPosition() != null) ? eye.distance(trace.getHitPosition().toLocation(world)) : maxDist;
        
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 230, 200), (float) (0.6 + Math.random() * 0.2));
        if (percent <= lowPercent) dust = new Particle.DustOptions(Color.fromRGB(200, 100, 50), 0.4f);

        for (double i = 0.5; i < endDist; i += 0.4) {
            Location pLoc = eye.clone().add(dir.clone().multiply(i));
            pLoc.add(Math.random()*0.1-0.05, Math.random()*0.1-0.05, Math.random()*0.1-0.05);
            world.spawnParticle(Particle.DUST, pLoc, 1, 0.0, 0.0, 0.0, 0.0, dust);
        }

        // 2. CONE RINGS (Peripheral light)
        if (ringLocs != null && !ringLocs.isEmpty()) {
            int rendered = 0;
            for (Location loc : ringLocs) {
                if (rendered >= 15) break; 
                RayTraceResult res = world.rayTraceBlocks(eye, loc.toVector().subtract(eye.toVector()).normalize(), eye.distance(loc), FluidCollisionMode.NEVER, true);
                if (res == null || res.getHitBlock() == null) {
                    world.spawnParticle(Particle.DUST, loc, 1, 0.05, 0.05, 0.05, 0.0, dust);
                    rendered++;
                }
            }
        }

        // 3. GLOW ENEMY
        Collection<Entity> near = world.getNearbyEntities(eye, maxDist, maxDist, maxDist, e -> e instanceof LivingEntity && e != player);
        for (Entity e : near) {
            Vector toEntity = e.getLocation().clone().add(0, 1, 0).toVector().subtract(eye.toVector());
            double dist = toEntity.length();
            if (dist > maxDist || dist < 1.0) continue;
            
            toEntity.normalize();
            double angle = Math.toDegrees(dir.angle(toEntity));
            if (angle <= (hAngle / 2.0) + 2) {
                LivingEntity le = (LivingEntity) e;
                le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10, 0, false, false));
            }
        }
    }

    public void spawnAtmosphericFog(Player player) {
        Location center = player.getLocation().add(0, 1.5, 0);
        double radius = 13.0; // Distance of the fog wall
        int points = 5; // Particle clusters per tick
        
        for (int i = 0; i < points; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = (Math.random() * 4.0) - 1.5; 
            
            Location pLoc = center.clone().add(x, y, z);
            // Spawn personal particles only visible to the player for maximum immersion and performance
            player.spawnParticle(Particle.LARGE_SMOKE, pLoc, 1, 0.4, 0.4, 0.4, 0.01);
            if (Math.random() < 0.2) {
                player.spawnParticle(Particle.SQUID_INK, pLoc, 1, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }
}
