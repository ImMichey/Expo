package dev.michey.expo.server.packet;

import dev.michey.expo.util.EntityRemovalReason;

public class P8_EntityDeleteStack extends Packet {

    public int[] entityList;
    public EntityRemovalReason[] reasons;
    public long timestamp;

}
