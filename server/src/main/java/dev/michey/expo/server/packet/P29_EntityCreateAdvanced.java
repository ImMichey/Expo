package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class P29_EntityCreateAdvanced extends Packet {

    public ServerEntityType entityType;
    public int entityId;
    public float entityHealth;

    public float serverPosX;
    public float serverPosY;
    public int tileArray;

    public Object[] payload;

}
