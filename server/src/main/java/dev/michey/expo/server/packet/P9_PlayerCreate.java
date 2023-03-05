package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class P9_PlayerCreate extends Packet {

    public ServerEntityType entityType;
    public int entityId;
    public String dimensionName;

    public float serverPosX;
    public float serverPosY;
    public int direction;

    public String username;
    public boolean player;

    public int[] equippedItemIds;
    public float armRotation;

    public float health;
    public float hunger;

}
