package dev.michey.expo.server.main.logic.inventory;

import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;

public class ServerInventorySlot {

    public int slotIndex;
    public ServerInventoryItem item;

    public ServerInventorySlot(int slotIndex) {
        this.slotIndex = slotIndex;
        item = new ServerInventoryItem();
    }

}
