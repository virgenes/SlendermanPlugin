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
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.utils.Util;
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

        // TOTAL ISOLATION: Block automated state updates for disguised players
        // This prevents the server from sending player-specific data that kicks clients
        
        List<PacketType> typeList = new ArrayList<>();
        typeList.add(PacketType.Play.Server.ENTITY_METADATA);
        typeList.add(PacketType.Play.Server.ENTITY_EQUIPMENT);
        typeList.add(PacketType.Play.Server.ENTITY_STATUS);
        typeList.add(PacketType.Play.Server.PLAYER_INFO);
        
        // Try to add PLAYER_INFO_UPDATE safely for compile-time compatibility
        try {
            // Using reflection to get the field to avoid compile errors if it's missing in some environments
            java.lang.reflect.Field field = PacketType.Play.Server.class.getField("PLAYER_INFO_UPDATE");
            typeList.add((PacketType) field.get(null));
        } catch (Throwable ignored) {}

        protocolManager.addPacketListener(new PacketAdapter(SlenderMain.getInstance(), ListenerPriority.NORMAL, typeList) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                PacketType type = event.getPacketType();

                if (type.name().contains("PLAYER_INFO")) {
                    // Filter player info updates (latency, game mode) for disguised players
                    try {
                        List<PlayerInfoData> dataList = packet.getPlayerInfoDataLists().read(0);
                        if (dataList != null) {
                            for (PlayerInfoData data : dataList) {
                                if (data != null) {
                                    UUID uuid = data.getProfileId(); 
                                    if (uuid == null && data.getProfile() != null) {
                                        uuid = data.getProfile().getUUID();
                                    }
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

                // Entity packets
                int entityId = packet.getIntegers().read(0);
                if (entityIdToUuid.containsKey(entityId)) {
                    // Block all automatic metadata, equipment, and status updates
                    event.setCancelled(true);
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
        Util.sendPluginMessage("&a[Disguise] " + target.getName() + " -> " + disguise.getDisplayName());
    }

    public static void undisguise(Player target) {
        if (target == null) return;
        UUID uuid = target.getUniqueId();
        if (activeDisguises.remove(uuid) == null) return;
        entityIdToUuid.remove(target.getEntityId());
        if (!ready || !target.isOnline()) return;

        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (!observer.equals(target)) {
                restorePlayer(target, observer);
            }
        }
    }

    public static void onPlayerJoin(Player newPlayer) {
        if (!ready) return;
        Bukkit.getScheduler().runTaskLater(SlenderMain.getInstance(), () -> {
            for (Map.Entry<UUID, ActiveDisguise> e : activeDisguises.entrySet()) {
                Player disguised = Bukkit.getPlayer(e.getKey());
                if (disguised == null || !disguised.isOnline() || disguised.equals(newPlayer)) continue;
                sendDisguise(disguised, e.getValue().type(), newPlayer);
            }
        }, 5L);
    }

    public static SlenderDisguise getDisguise(UUID uuid) {
        ActiveDisguise a = activeDisguises.get(uuid);
        return a != null ? a.type() : null;
    }

    public static void undisguiseAll() {
        for (UUID uuid : new ArrayList<>(activeDisguises.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                undisguise(p);
            } else {
                activeDisguises.remove(uuid);
                // Clean up by iterating values if player is offline
                entityIdToUuid.values().remove(uuid);
            }
        }
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

            // 1. Remove player from tab list
            PacketContainer removeInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            removeInfo.getUUIDLists().write(0, List.of(target.getUniqueId()));
            send(observer, removeInfo);

            // 2. Destroy player entity
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, List.of(entityId));
            send(observer, destroy);

            // 3. Spawn mob with same entity ID, random UUID
            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, entityId);
            spawn.getUUIDs().write(0, UUID.randomUUID());
            spawn.getEntityTypeModifier().write(0, toBukkitEntityType(disguise));
            spawn.getDoubles().write(0, x);
            spawn.getDoubles().write(1, y);
            spawn.getDoubles().write(2, z);
            spawn.getBytes().write(0, angleToByte(xRot));
            spawn.getBytes().write(1, angleToByte(yRot));
            spawn.getBytes().write(2, angleToByte(yHead));
            // In 1.21, index 1 is Object Data (optional, usually 0 for most mobs)
            spawn.getIntegers().write(1, 0);
            send(observer, spawn);

            // 4. Send INITIAL metadata packet (Required for 1.19.3+ / 1.21 mobs to not kick)
            PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadata.getIntegers().write(0, entityId);
            
            // For 1.19.3+ we use WrappedDataValue
            List<WrappedDataValue> dataValues = new ArrayList<>();
            // Index 0 = Status (on fire, etc.). Byte 0 is safe.
            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));
            
            metadata.getDataValueCollectionModifier().write(0, dataValues);
            send(observer, metadata);

        } catch (Exception e) {
            Util.sendPluginMessage("&c[Disguise] sendDisguise error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void restorePlayer(Player target, Player observer) {
        try {
            Object nmsTarget = getHandleMethod.invoke(target);
            int    entityId  = (int)    getIdMethod.invoke(nmsTarget);
            double x         = (double) getXMethod.invoke(nmsTarget);
            double y         = (double) getYMethod.invoke(nmsTarget);
            double z         = (double) getZMethod.invoke(nmsTarget);
            float  xRot      = (float)  getXRotMethod.invoke(nmsTarget);
            float  yRot      = (float)  getYRotMethod.invoke(nmsTarget);
            float  yHead     = (float)  getYHeadRotMethod.invoke(nmsTarget);

            // Destroy fake mob
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, List.of(entityId));
            send(observer, destroy);

            // Re-add player to tab list for restoration
            try {
                // In 1.21, PLAYER_INFO is mostly replaced by PLAYER_INFO_UPDATE in ProtocolLib mapping
                PacketContainer addInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
                addInfo.getPlayerInfoActions().write(0, java.util.EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER, EnumWrappers.PlayerInfoAction.UPDATE_LISTED));
                WrappedGameProfile profile = WrappedGameProfile.fromPlayer(target);
                addInfo.getPlayerInfoDataLists().write(0, List.of(
                    new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.ADVENTURE, null)
                ));
                send(observer, addInfo);
            } catch (Exception ignored) {}

            // Re-spawn real player
            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, entityId);
            spawn.getUUIDs().write(0, target.getUniqueId());
            spawn.getEntityTypeModifier().write(0, EntityType.PLAYER);
            spawn.getDoubles().write(0, x);
            spawn.getDoubles().write(1, y);
            spawn.getDoubles().write(2, z);
            spawn.getBytes().write(0, angleToByte(xRot));
            spawn.getBytes().write(1, angleToByte(yRot));
            spawn.getBytes().write(2, angleToByte(yHead));
            spawn.getIntegers().write(1, 0);
            send(observer, spawn);

            // Send Metadata for player
            PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadata.getIntegers().write(0, entityId);
            
            List<WrappedDataValue> dataValues = new ArrayList<>();
            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));
            
            metadata.getDataValueCollectionModifier().write(0, dataValues);
            send(observer, metadata);

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
