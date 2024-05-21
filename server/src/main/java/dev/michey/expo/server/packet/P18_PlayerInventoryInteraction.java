package dev.michey.expo.server.packet;

public class P18_PlayerInventoryInteraction extends Packet {

    public int actionType;
    public int containerId;
    public int slotId;
    public boolean shift;

}
