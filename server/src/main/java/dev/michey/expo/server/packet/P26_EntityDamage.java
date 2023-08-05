package dev.michey.expo.server.packet;

public class P26_EntityDamage extends Packet {

    public int entityId;
    public float damage;
    public float newHealth;
    public int damageSourceEntityId;

}
