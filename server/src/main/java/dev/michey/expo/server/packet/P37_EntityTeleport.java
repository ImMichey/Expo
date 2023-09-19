package dev.michey.expo.server.packet;

import dev.michey.expo.server.util.TeleportReason;

public class P37_EntityTeleport extends Packet {

    public int entityId;
    public float x;
    public float y;
    public TeleportReason teleportReason;

}
