package dev.michey.expo.server.packet;

import dev.michey.expo.util.EntityRemovalReason;

public class P43_EntityDeleteAdvanced extends Packet {

    public int entityId;
    public EntityRemovalReason reason;

    public float damage;
    public float newHealth;
    public int damageSourceEntityId;

}