package me.dreamdevs.slender.disguise;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import me.dreamdevs.slender.SlenderMain;
import me.dreamdevs.slender.api.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Disguise system using ProtocolLib.
 * Sends packets below ViaVersion — no protocol errors, no kicks.
 */
public class DisguiseManager {

    private record ActiveDisguise(SlenderDisguise type) {}

    private static final Map<UUID, ActiveDisguise> activeDisguises = new HashMap<>();
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
        } catch (Exception e) {
            Util.sendPluginMessage("&e[Disguise] Init failed: " + e.getMessage());
        }
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
        Util.sendPluginMessage("&a[Disguise] " + target.getName() + " -> " + disguise.getDisplayName());
    }

    public static void undisguise(Player target) {
        if (target == null) return;
        if (activeDisguises.remove(target.getUniqueId()) == null) return;
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
            if (p != null) undisguise(p);
            else activeDisguises.remove(uuid);
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
            spawn.getIntegers().write(1, 0);
            spawn.getIntegers().write(2, 0);
            spawn.getIntegers().write(3, 0);
            spawn.getIntegers().write(4, 0);
            send(observer, spawn);

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

            // Re-add player to tab list
            try {
                PacketContainer addInfo = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
                addInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
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
            spawn.getIntegers().write(2, 0);
            spawn.getIntegers().write(3, 0);
            spawn.getIntegers().write(4, 0);
            send(observer, spawn);

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
