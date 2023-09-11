package dev.michey.expo.logic.inventory;

import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;

public class ClientInventorySlot {

    public int slotIndex;
    public ClientInventoryItem item;

    public ClientInventorySlot(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public ClientInventorySlot(ServerInventorySlot serverSlot) {
        slotIndex = serverSlot.slotIndex;
        item = ClientInventoryItem.from(serverSlot.item);
    }

}
