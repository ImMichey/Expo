package dev.michey.expo.server.packet;

import dev.michey.expo.util.EntityRemovalReason;

public class P4_EntityDelete extends Packet {

    public int entityId;
    public EntityRemovalReason reason;

}
