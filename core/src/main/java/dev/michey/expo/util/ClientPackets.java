package dev.michey.expo.util;

import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.server.packet.*;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientPackets {

    /** Sends the P0_Auth_Req packet via TCP protocol. */
    public static void p0Auth(String username) {
        P0_Auth_Req p = new P0_Auth_Req();
        p.username = username;
        tcp(p);
    }

    /** Sends the P5_PlayerVelocity packet via UDP protocol. */
    public static void p5PlayerVelocity(int xDir, int yDir) {
        P5_PlayerVelocity p = new P5_PlayerVelocity();
        p.xDir = xDir;
        p.yDir = yDir;
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
    public static void p18PlayerInventoryInteraction(int actionType, int slotId) {
        P18_PlayerInventoryInteraction p = new P18_PlayerInventoryInteraction();
        p.actionType = actionType;
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

    /** Helper methods */
    private static void tcp(Packet p) {
        ExpoClientContainer.get().sendPacketTcp(p);
    }

    private static void udp(Packet p) {
        ExpoClientContainer.get().sendPacketUdp(p);
    }

}
