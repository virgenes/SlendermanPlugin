package me.dreamdevs.slender.disguise;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.listeners.GameListeners;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.lang.reflect.Method;

/**
 * Disguise system using ProtocolLib.
 * Sends packets below ViaVersion — no protocol errors, no kicks.
 */
public class DisguiseManager {
    private record ActiveDisguise(SlenderDisguise type) {}

    private static final int ID_OFFSET = 200000;
    private static final Map<UUID, ActiveDisguise> activeDisguises = new HashMap<>();
    private static final Map<Integer, UUID> entityIdToUuid = new HashMap<>(); // For O(1) packet lookup
    private static ProtocolManager protocolManager;
    private static boolean ready = false;

    // NMS reflection
    private static Method getHandleMethod;
    private static Method getIdMethod;
    private static Method getXMethod, getYMethod, getZMethod;
    private static Method getXRotMethod, getYRotMethod, getYHeadRotMethod;

    public static void init() {
        try {
            protocolManager = ProtocolLibrary.getProtocolManager();

            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            Class<?> nmsEntity   = Class.forName("net.minecraft.world.entity.Entity");

            getHandleMethod  = craftPlayer.getMethod("getHandle");
            getIdMethod      = nmsEntity.getMethod("getId");
            getXMethod       = nmsEntity.getMethod("getX");
            getYMethod       = nmsEntity.getMethod("getY");
            getZMethod       = nmsEntity.getMethod("getZ");
            getXRotMethod    = nmsEntity.getMethod("getXRot");
            getYRotMethod    = nmsEntity.getMethod("getYRot");
            getYHeadRotMethod = nmsEntity.getMethod("getYHeadRot");

            ready = true;
            Util.sendPluginMessage("&a[Disguise] ProtocolLib disguise ready.");
            registerPacketListeners();
        } catch (Exception e) {
            Util.sendPluginMessage("&e[Disguise] Init failed: " + e.getMessage());
        }
    }

    private static void registerPacketListeners() {
        if (!ready) return;

        // TOTAL ISOLATION: Rewrite ALL packets related to disguised players to use a virtual ID
        // This bypasses ViaVersion's entity map entirely.
        
        List<PacketType> typeList = new ArrayList<>();
        // Movement
        typeList.add(PacketType.Play.Server.REL_ENTITY_MOVE);
        typeList.add(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        typeList.add(PacketType.Play.Server.ENTITY_LOOK);
        typeList.add(PacketType.Play.Server.ENTITY_TELEPORT);
        typeList.add(PacketType.Play.Server.ENTITY_VELOCITY);
        typeList.add(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        typeList.add(PacketType.Play.Server.SPAWN_ENTITY);
        // Status/Updates
        typeList.add(PacketType.Play.Server.ENTITY_METADATA);
        typeList.add(PacketType.Play.Server.ENTITY_EQUIPMENT);
        typeList.add(PacketType.Play.Server.ENTITY_STATUS);
        typeList.add(PacketType.Play.Server.ANIMATION);
        // Player Info
        typeList.add(PacketType.Play.Server.PLAYER_INFO);
        
        try {
            java.lang.reflect.Field field = PacketType.Play.Server.class.getField("PLAYER_INFO_UPDATE");
            typeList.add((PacketType) field.get(null));
        } catch (Throwable ignored) {}

        protocolManager.addPacketListener(new PacketAdapter(SlenderMain.getInstance(), ListenerPriority.HIGHEST, typeList) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                PacketType type = event.getPacketType();

                if (type.name().contains("PLAYER_INFO")) {
                    try {
                        List<PlayerInfoData> dataList = packet.getPlayerInfoDataLists().read(0);
                        if (dataList != null) {
                            for (PlayerInfoData data : dataList) {
                                if (data != null) {
                                    UUID uuid = data.getProfileId(); 
                                    if (uuid == null && data.getProfile() != null) uuid = data.getProfile().getUUID();
                                    if (uuid != null && activeDisguises.containsKey(uuid)) {
                                        event.setCancelled(true);
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    return;
                }

                // Entity ID Rewriting: Shadowing movement for the Virtual ID
                try {
                    int entityId = packet.getIntegers().read(0);
                    
                    if (entityIdToUuid.containsKey(entityId)) {
                        // 1. If it's a SPAWN_ENTITY for the real player, CANCEL it for others
                        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
                            event.setCancelled(true);
                            return;
                        }

                        // 2. REWRITE ID TO VIRTUAL OFFSET for other packets
                        packet.getIntegers().write(0, entityId + ID_OFFSET);
                        
                        // If metadata, also rewrite content to safe mob values
                        if (type == PacketType.Play.Server.ENTITY_METADATA) {
                            List<WrappedDataValue> dataValues = new ArrayList<>();
                            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));
                            dataValues.add(new WrappedDataValue(1, WrappedDataWatcher.Registry.get(Integer.class), 300));
                            dataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), false));
                            dataValues.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), false));
                            
                            SlenderDisguise disguise = getDisguise(entityIdToUuid.get(entityId));
                            if (disguise == SlenderDisguise.ENDERMAN || disguise == null) {
                                dataValues.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Boolean.class), false));
                                dataValues.add(new WrappedDataValue(18, WrappedDataWatcher.Registry.get(Boolean.class), false));
                            }
                            packet.getDataValueCollectionModifier().write(0, dataValues);
                        } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
                            // Block items for mob
                            event.setCancelled(true);
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        // INBOUND: Handle Survivors hitting the Virtual Slender ID
        protocolManager.addPacketListener(new PacketAdapter(SlenderMain.getInstance(), ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                int virtualId = packet.getIntegers().read(0);

                // Only handle IDs in our virtual range
                if (virtualId >= ID_OFFSET) {
                    int realId = virtualId - ID_OFFSET;
                    UUID uuid = entityIdToUuid.get(realId);
                    if (uuid != null) {
                        Player victim = Bukkit.getPlayer(uuid);
                        Player damager = event.getPlayer();

                        if (victim != null && damager != null) {
                            // Check if it's an ATTACK (left click)
                            try {
                                WrappedEnumEntityUseAction wrappedAction = packet.getEnumEntityUseActions().read(0);
                                EnumWrappers.EntityUseAction action = wrappedAction.getAction();
                                
                                if (action == EnumWrappers.EntityUseAction.ATTACK) {
                                    // Must run stun logic on main thread
                                    Bukkit.getScheduler().runTask(SlenderMain.getInstance(), () -> {
                                        GameListeners.handleSurvivorSwordHit(damager, victim);
                                    });
                                }
                            } catch (Exception e) {
                                // Fallback for older ProtocolLib or non-wrapped actions if any
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
    }

    public static void disguise(Player target, SlenderDisguise disguise) {
        if (!ready || target == null || !target.isOnline()) return;
        undisguise(target);

        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (!observer.equals(target)) {
                sendDisguise(target, disguise, observer);
            }
        }

        activeDisguises.put(target.getUniqueId(), new ActiveDisguise(disguise));
        entityIdToUuid.put(target.getEntityId(), target.getUniqueId());
        Util.sendPluginMessage("&a[Disguise] " + target.getName() + " -> " + disguise.getDisplayName() + " (VirtualID: " + (target.getEntityId() + ID_OFFSET) + ")");
    }

    public static void undisguise(Player target) {
        if (target == null) return;
        UUID uuid = target.getUniqueId();
        if (activeDisguises.remove(uuid) == null) return;
        
        if (!ready || !target.isOnline()) {
            entityIdToUuid.remove(target.getEntityId());
            return;
        }

        int entityId = target.getEntityId();
        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (!observer.equals(target)) {
                restorePlayer(target, observer);
            }
        }
        entityIdToUuid.remove(entityId);
    }

    public static void onPlayerJoin(Player newPlayer) {
        if (!ready) return;
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
            for (Map.Entry<UUID, ActiveDisguise> e : activeDisguises.entrySet()) {
                Player disguised = Bukkit.getPlayer(e.getKey());
                if (disguised == null || !disguised.isOnline() || disguised.equals(newPlayer)) continue;
                sendDisguise(disguised, e.getValue().type(), newPlayer);
            }
        }, 12L);
    }

    public static SlenderDisguise getDisguise(UUID uuid) {
        ActiveDisguise a = activeDisguises.get(uuid);
        return a != null ? a.type() : null;
    }

    public static void undisguiseAll() {
        for (UUID uuid : new ArrayList<>(activeDisguises.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) undisguise(p);
            else activeDisguises.remove(uuid);
        }
        entityIdToUuid.clear();
    }

    private static void sendDisguise(Player target, SlenderDisguise disguise, Player observer) {
        try {
            Object nmsTarget = getHandleMethod.invoke(target);
            int    entityId  = (int)    getIdMethod.invoke(nmsTarget);
            double x         = (double) getXMethod.invoke(nmsTarget);
            double y         = (double) getYMethod.invoke(nmsTarget);
            double z         = (double) getZMethod.invoke(nmsTarget);
            float  xRot      = (float)  getXRotMethod.invoke(nmsTarget);
            float  yRot      = (float)  getYRotMethod.invoke(nmsTarget);
            float  yHead     = (float)  getYHeadRotMethod.invoke(nmsTarget);

            // 1. Shadow: Remove player from tab list
            PacketContainer removeInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            removeInfo.getUUIDLists().write(0, List.of(target.getUniqueId()));
            send(observer, removeInfo);

            // 2. Hide original player entity (Already handled by hidePlayer usually, but we ensure destruction)
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, List.of(entityId));
            send(observer, destroy);

            // 3. Spawn Virtual Mob with ID OFFSET (Virtual ID = realID + 200,000)
            // This entity ID never belonged to a player, avoiding ViaVersion conflicts.
            int virtualId = entityId + ID_OFFSET;
            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, virtualId);
            spawn.getUUIDs().write(0, UUID.randomUUID());
            spawn.getEntityTypeModifier().write(0, toBukkitEntityType(disguise));
            spawn.getDoubles().write(0, x);
            spawn.getDoubles().write(1, y);
            spawn.getDoubles().write(2, z);
            spawn.getBytes().write(0, angleToByte(xRot));
            spawn.getBytes().write(1, angleToByte(yRot));
            spawn.getBytes().write(2, angleToByte(yHead));
            spawn.getIntegers().write(1, 0);
            send(observer, spawn);

            // 4. Initial Virtual Rotation
            PacketContainer headRotation = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            headRotation.getIntegers().write(0, virtualId);
            headRotation.getBytes().write(0, angleToByte(yHead));
            send(observer, headRotation);

            // 5. Initial Virtual Metadata
            PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadata.getIntegers().write(0, virtualId);
            List<WrappedDataValue> dataValues = new ArrayList<>();
            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));
            dataValues.add(new WrappedDataValue(1, WrappedDataWatcher.Registry.get(Integer.class), 300));
            dataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), false));
            dataValues.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), false));
            if (disguise == SlenderDisguise.ENDERMAN || disguise == null) {
                dataValues.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Boolean.class), false));
                dataValues.add(new WrappedDataValue(18, WrappedDataWatcher.Registry.get(Boolean.class), false));
            }
            metadata.getDataValueCollectionModifier().write(0, dataValues);
            send(observer, metadata);

            Bukkit.getLogger().info("[Disguise] sendDisguise (VirtualId: " + virtualId + ") sent to " + observer.getName());

        } catch (Exception e) {
            Util.sendPluginMessage("&c[Disguise] sendDisguise error: " + e.getMessage());
        }
    }

    private static void restorePlayer(Player target, Player observer) {
        try {
            int entityId = target.getEntityId();
            int virtualId = entityId + ID_OFFSET;

            // Destroy Virtual Entity
            PacketContainer destroyVirtual = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyVirtual.getIntLists().write(0, List.of(virtualId));
            send(observer, destroyVirtual);

            // The real player should be restored by Bukkit's showPlayer, 
            // but for safety in same-tick we can rely on standard showPlayer calls.

        } catch (Exception ignored) {}
    }

    private static void send(Player observer, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(observer, packet);
        } catch (Exception e) {
            Util.sendPluginMessage("&c[Disguise] send error: " + e.getMessage());
        }
    }

    private static byte angleToByte(float angle) {
        return (byte) Math.floor(angle * 256f / 360f);
    }

    private static EntityType toBukkitEntityType(SlenderDisguise disguise) {
        if (disguise == null) return EntityType.ENDERMAN;
        return switch (disguise) {
            case WITHER         -> EntityType.WITHER;
            case PHANTOM        -> EntityType.PHANTOM;
            case RAVAGER        -> EntityType.RAVAGER;
            case ELDER_GUARDIAN -> EntityType.ELDER_GUARDIAN;
            case WARDEN         -> EntityType.WARDEN;
            default             -> EntityType.ENDERMAN;
        };
    }
}
