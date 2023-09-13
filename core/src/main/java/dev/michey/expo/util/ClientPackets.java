package dev.michey.expo.util;

import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.server.packet.*;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientPackets {

    /** Sends the P0_Auth_Req packet via TCP protocol. */
    public static void p0Auth(String username) {
        P0_Auth_Req p = new P0_Auth_Req();
        p.username = username;
        p.protocolVersion = ExpoShared.SERVER_PROTOCOL_VERSION;
        tcp(p);
    }

    /** Sends the P5_PlayerVelocity packet via UDP protocol. */
    public static void p5PlayerVelocity(int xDir, int yDir, boolean sprinting) {
        P5_PlayerVelocity p = new P5_PlayerVelocity();
        p.xDir = xDir;
        p.yDir = yDir;
        p.sprinting = sprinting;
        udp(p);
    }

    /** Sends the P12_PlayerDirection packet via UDP protocol (entityId is not required as it's handled by the server). */
    public static void p12PlayerDirection(int direction) {
        P12_PlayerDirection p = new P12_PlayerDirection();
        p.entityId = -1;
        p.direction = direction;
        udp(p);
    }

    /** Sends the P16_PlayerPunch packet via UDP protocol.  */
    public static void p16PlayerPunch(float angle) {
        P16_PlayerPunch p = new P16_PlayerPunch();
        p.punchAngle = angle;
        udp(p);
    }

    /** Sends the P18_PlayerInventoryInteraction packet via UDP protocol. */
    public static void p18PlayerInventoryInteraction(int actionType, int containerId, int slotId) {
        P18_PlayerInventoryInteraction p = new P18_PlayerInventoryInteraction();
        p.actionType = actionType;
        p.containerId = containerId;
        p.slotId = slotId;
        udp(p);
    }

    /** Sends the P20_PlayerInventorySwitch packet via UDP protocol. */
    public static void p20PlayerInventorySwitch(int slotId) {
        P20_PlayerInventorySwitch p = new P20_PlayerInventorySwitch();
        p.slot = slotId;
        udp(p);
    }

    /** Sends the P22_PlayerArmDirection packet via UDP protocol. */
    public static void p22PlayerArmDirection(float rotation) {
        P22_PlayerArmDirection p = new P22_PlayerArmDirection();
        p.rotation = rotation;
        udp(p);
    }

    /** Sends the P25_ChatMessage packet via TCP protocol. */
    public static void p25ChatMessage(String message) {
        P25_ChatMessage p = new P25_ChatMessage();
        p.message = message;
        p.sender = ClientStatic.PLAYER_USERNAME;
        tcp(p);
    }

    /** Sends the P27_PlayerEntitySelection packet via UDP protocol. */
    public static void p27PlayerEntitySelection(int entityId) {
        P27_PlayerEntitySelection p = new P27_PlayerEntitySelection();
        p.entityId = entityId;
        udp(p);
    }

    /** Sends the P31_PlayerDig packet via UDP protocol. */
    public static void p31PlayerDig(int chunkX, int chunkY, int tileX, int tileY, int tileArray) {
        P31_PlayerDig p = new P31_PlayerDig();
        p.chunkX = chunkX;
        p.chunkY = chunkY;
        p.tileX = tileX;
        p.tileY = tileY;
        p.tileArray = tileArray;
        udp(p);
    }

    /** Sends the P34_PlayerPlace packet via UDP protocol. */
    public static void p34PlayerPlace(int chunkX, int chunkY, int tileX, int tileY, int tileArray, float mouseWorldX, float mouseWorldY) {
        P34_PlayerPlace p = new P34_PlayerPlace();
        p.chunkX = chunkX;
        p.chunkY = chunkY;
        p.tileX = tileX;
        p.tileY = tileY;
        p.tileArray = tileArray;
        p.mouseWorldX = mouseWorldX;
        p.mouseWorldY = mouseWorldY;
        udp(p);
    }

    /** Sends the P35_PlayerCraft packet via UDP protocol. */
    public static void p35PlayerCraft(String recipeIdentifier, boolean all) {
        P35_PlayerCraft p = new P35_PlayerCraft();
        p.recipeIdentifier = recipeIdentifier;
        p.all = all;
        udp(p);
    }

    /** Sends the P39_PlayerInteractEntity packet via UDP protocol. */
    public static void p39PlayerInteractEntity() {
        P39_PlayerInteractEntity p = new P39_PlayerInteractEntity();
        p.entityId = ClientEntityManager.get().selectedEntity.entityId;
        udp(p);
    }

    /** Sends the P41_InventoryViewQuit packet via TCP protocol. */
    public static void p41InventoryViewQuit() {
        P41_InventoryViewQuit p = new P41_InventoryViewQuit();
        tcp(p);
    }

    /** Helper methods */
    private static void tcp(Packet p) {
        ExpoClientContainer.get().sendPacketTcp(p);
    }

    private static void udp(Packet p) {
        ExpoClientContainer.get().sendPacketUdp(p);
    }

}
