package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class P2_EntityCreate extends Packet {

    public ServerEntityType entityType;
    public int entityId;
    public String dimensionName;
    public float entityHealth;

    public float serverPosX;
    public float serverPosY;
    public int tileArray;

}