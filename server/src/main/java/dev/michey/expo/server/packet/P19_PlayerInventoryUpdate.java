package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;

public class P19_PlayerInventoryUpdate extends Packet {

    public int[] updatedSlots;
    public ServerInventoryItem[] updatedItems;

}
