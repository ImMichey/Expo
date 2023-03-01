package dev.michey.expo.server.main.logic.inventory;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;

public class ServerInventory {

    /** What entity does the inventory belong to (player, mob, chest, etc.)? */
    private ServerEntity inventoryOwner = null;

    /** Data structure */
    public final ServerInventorySlot[] slots;

    public ServerInventory(int size) {
        slots = new ServerInventorySlot[size];

        for(int i = 0; i < slots.length; i++) {
            slots[i] = new ServerInventorySlot(i);
        }
    }

    public boolean isEmpty() {
        for(ServerInventorySlot slot : slots) {
            if(!slot.item.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public boolean hasOwner() {
        return inventoryOwner != null;
    }

    public void setOwner(ServerEntity owner) {
        this.inventoryOwner = owner;
    }

    public ServerEntity getOwner() {
        return inventoryOwner;
    }

}