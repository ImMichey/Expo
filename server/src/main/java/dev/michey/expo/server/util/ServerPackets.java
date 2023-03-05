package dev.michey.expo.server.util;

import com.esotericsoftware.kryonet.Connection;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.connection.PlayerConnection;
import dev.michey.expo.server.connection.PlayerConnectionHandler;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.ServerInventory;
import dev.michey.expo.server.main.logic.inventory.ServerPlayerInventory;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.packet.*;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerPackets {

    /** Sends the P1_Auth_Rsp packet via TCP protocol. */
    public static void p1AuthResponse(boolean authorized, String message, int serverTps, PacketReceiver receiver) {
        P1_Auth_Rsp p = new P1_Auth_Rsp();
        p.authorized = authorized;
        p.message = message;
        p.serverTps = serverTps;
        tcp(p, receiver);
    }

    /** Sends the P2_EntityCreate packet via TCP protocol. */
    public static void p2EntityCreate(ServerEntityType entityType, int entityId, String dimensionName, float serverPosX, float serverPosY, PacketReceiver receiver) {
        P2_EntityCreate p = new P2_EntityCreate();
        p.entityType = entityType;
        p.entityId = entityId;
        p.dimensionName = dimensionName;
        p.serverPosX = serverPosX;
        p.serverPosY = serverPosY;
        tcp(p, receiver);
    }

    public static void p2EntityCreate(ServerEntity entity, PacketReceiver receiver) {
        p2EntityCreate(entity.getEntityType(), entity.entityId, entity.entityDimension, entity.posX, entity.posY, receiver);
    }

    /** Sends the P3_PlayerJoin packet via TCP protocol. */
    public static void p3PlayerJoin(String username, PacketReceiver receiver) {
        P3_PlayerJoin p = new P3_PlayerJoin();
        p.username = username;
        tcp(p, receiver);
    }

    /** Sends the P4_EntityDelete packet via TCP protocol. */
    public static void p4EntityDelete(int entityId, PacketReceiver receiver) {
        P4_EntityDelete p = new P4_EntityDelete();
        p.entityId = entityId;
        tcp(p, receiver);
    }

    /** Sends the P6_EntityPosition packet via UDP protocol. */
    public static void p6EntityPosition(int entityId, float xPos, float yPos, PacketReceiver receiver) {
        P6_EntityPosition p = new P6_EntityPosition();
        p.entityId = entityId;
        p.xPos = xPos;
        p.yPos = yPos;
        udp(p, receiver);
    }

    /** Sends the P7_ChunkSnapshot packet via UDP protocol. */
    public static void p7ChunkSnapshot(int[] activeChunks, PacketReceiver receiver) {
        P7_ChunkSnapshot p = new P7_ChunkSnapshot();
        p.activeChunks = activeChunks;
        udp(p, receiver);
    }

    /** Sends the P8_EntityDeleteStack packet via TCP protocol. */
    public static void p8EntityDeleteStack(int[] entityList, PacketReceiver receiver) {
        P8_EntityDeleteStack p = new P8_EntityDeleteStack();
        p.entityList = entityList;
        tcp(p, receiver);
    }

    /** Sends the P9_PlayerCreate packet via TCP protocol. */
    public static void p9PlayerCreate(int entityId, String dimensionName, float serverPosX, float serverPosY, int direction, String username, boolean player, int[] equippedItemIds, float armRotation, PacketReceiver receiver) {
        P9_PlayerCreate p = new P9_PlayerCreate();
        p.entityType = ServerEntityType.PLAYER;
        p.entityId = entityId;
        p.dimensionName = dimensionName;
        p.serverPosX = serverPosX;
        p.serverPosY = serverPosY;
        p.username = username;
        p.player = player;
        p.direction = direction;
        p.equippedItemIds = equippedItemIds;
        p.armRotation = armRotation;
        tcp(p, receiver);
    }

    public static void p9PlayerCreate(ServerPlayer entity, boolean player, PacketReceiver receiver) {
        p9PlayerCreate(entity.entityId, entity.entityDimension, entity.posX, entity.posY, entity.playerDirection, entity.username, player, entity.getEquippedItemIds(), entity.serverArmRotation, receiver);
    }

    /** Sends the P10_PlayerQuit packet via TCP protocol. */
    public static void p10PlayerQuit(String username, PacketReceiver receiver) {
        P10_PlayerQuit p = new P10_PlayerQuit();
        p.username = username;
        tcp(p, receiver);
    }

    /** Sends the P11_ChunkData packet via TCP protocol. */
    public static void p11ChunkData(int chunkX, int chunkY, BiomeType[] biomeData, int[] tileIndexData, boolean[] waterLoggedData, PacketReceiver receiver) {
        P11_ChunkData p = new P11_ChunkData();
        p.chunkX = chunkX;
        p.chunkY = chunkY;
        p.biomeData = biomeData;
        p.tileIndexData = tileIndexData;
        p.waterLoggedData = waterLoggedData;
        tcp(p, receiver);
    }

    /** Sends the P12_PlayerDirection packet via UDP protocol. */
    public static void p12PlayerDirection(int entityId, int direction, PacketReceiver receiver) {
        P12_PlayerDirection p = new P12_PlayerDirection();
        p.entityId = entityId;
        p.direction = direction;
        tcp(p, receiver);
    }

    /** Sends the P13_EntityMove packet via UDP protocol. */
    public static void p13EntityMove(int entityId, int xDir, int yDir, float xPos, float yPos, PacketReceiver receiver) {
        P13_EntityMove p = new P13_EntityMove();
        p.entityId = entityId;
        p.xDir = xDir;
        p.yDir = yDir;
        p.xPos = xPos;
        p.yPos = yPos;
        udp(p, receiver);
    }

    /** Sends the P14_WorldUpdate packet via TCP protocol. */
    public static void p14WorldUpdate(float worldTime, int worldWeather, PacketReceiver receiver) {
        P14_WorldUpdate p = new P14_WorldUpdate();
        p.worldTime = worldTime;
        p.worldWeather = worldWeather;
        tcp(p, receiver);
    }

    /** Sends the P15_PingList packet via TCP protocol. */
    public static void p15PingList(PacketReceiver receiver) {
        var map = PlayerConnectionHandler.get().connections();
        int size = map.size();
        P15_PingList p = new P15_PingList();
        p.username = new String[size];
        p.ping = new int[size];

        int index = 0;

        for(PlayerConnection con : map) {
            p.username[index] = con.player.username;
            p.ping[index] = con.getKryoConnection().getReturnTripTime();
            index++;
        }

        tcp(p, receiver);
    }

    /** Sends the P17_PlayerPunchData packet via UDP protocol. */
    public static void p17PlayerPunchData(int entityId, float punchAngleStart, float punchAngleEnd, float punchDuration, PacketReceiver receiver) {
        P17_PlayerPunchData p = new P17_PlayerPunchData();
        p.entityId = entityId;
        p.punchAngleStart = punchAngleStart;
        p.punchAngleEnd = punchAngleEnd;
        p.punchDuration = punchDuration;
        udp(p, receiver);
    }

    /** Sends the P19_PlayerInventoryUpdate packet via TCP protocol. */
    public static void p19PlayerInventoryUpdate(int[] updatedSlots, ServerInventoryItem[] updatedItems, PacketReceiver receiver) {
        P19_PlayerInventoryUpdate p = new P19_PlayerInventoryUpdate();
        p.updatedSlots = updatedSlots;
        p.updatedItems = updatedItems;
        tcp(p, receiver);
    }

    /** Sends the P19_PlayerInventoryUpdate packet via TCP protocol. */
    public static void p19PlayerInventoryUpdate(ServerPlayer player, PacketReceiver receiver) {
        ServerPlayerInventory inv = player.playerInventory;
        int[] updatedSlots = new int[inv.slots.length];
        for(int i = 0; i < updatedSlots.length; i++) updatedSlots[i] = i;
        ServerInventoryItem[] updated = new ServerInventoryItem[inv.slots.length];
        for(int i = 0; i < updated.length; i++) updated[i] = inv.slots[i].item;
        p19PlayerInventoryUpdate(updatedSlots, updated, receiver);
    }

    /** Sends the P21_PlayerGearUpdate packet via UDP protocol. */
    public static void p21PlayerGearUpdate(int entityId, int[] heldItemIds, PacketReceiver receiver) {
        P21_PlayerGearUpdate p = new P21_PlayerGearUpdate();
        p.entityId = entityId;
        p.heldItemIds = heldItemIds;
        udp(p, receiver);
    }

    /** Sends the P22_PlayerArmDirection packet via UDP protocol. */
    public static void p22PlayerArmDirection(int entityId, float rotation, PacketReceiver receiver) {
        P22_PlayerArmDirection p = new P22_PlayerArmDirection();
        p.entityId = entityId;
        p.rotation = rotation;
        udp(p, receiver);
    }

    /** Sends the P23_PlayerLifeUpdate packet via UDP protocol. */
    public static void p23PlayerLifeUpdate(float health, float hunger, PacketReceiver receiver) {
        P23_PlayerLifeUpdate p = new P23_PlayerLifeUpdate();
        p.health = health;
        p.hunger = hunger;
        udp(p, receiver);
    }

    /** Helper methods below. */
    private static void udp(Packet p, PacketReceiver receiver) {
        if(receiver == null) return;

        if(receiver.all) {
            ExpoServerBase.get().broadcastPacketUDP(p);
            return;
        }

        if(receiver.receiverList != null) {
            for(Connection con : receiver.receiverList) {
                con.sendUDP(p);
            }

            return;
        }

        if(receiver.receiverSingle == null) {
            ExpoServerBase.get().broadcastPacketUDP(p);
        } else {
            receiver.receiverSingle.sendUDP(p);
        }
    }

    private static void tcp(Packet p, PacketReceiver receiver) {
        if(receiver == null) return;

        if(receiver.all) {
            ExpoServerBase.get().broadcastPacketTCP(p);
            return;
        }

        if(receiver.receiverList != null) {
            for(Connection con : receiver.receiverList) {
                con.sendTCP(p);
            }

            return;
        }

        if(receiver.receiverSingle == null) {
            ExpoServerBase.get().broadcastPacketTCP(p);
        } else {
            receiver.receiverSingle.sendTCP(p);
        }
    }

}
