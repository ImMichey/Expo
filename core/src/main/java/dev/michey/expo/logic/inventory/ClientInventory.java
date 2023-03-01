package dev.michey.expo.logic.inventory;

import dev.michey.expo.logic.entity.arch.ClientEntity;

public class ClientInventory {

    /** What entity does the inventory belong to (player, mob, chest, etc.)? */
    private ClientEntity inventoryOwner = null;

    /** Data structure */
    private final ClientInventorySlot[] slots;

    public ClientInventory(int size) {
        slots = new ClientInventorySlot[size];

        for(int i = 0; i < slots.length; i++) {
            slots[i] = new ClientInventorySlot(i);
        }
    }

    public boolean isEmpty() {
        for(ClientInventorySlot slot : slots) {
            if(slot.item != null) {
                return false;
            }
        }

        return true;
    }

    public boolean hasOwner() {
        return inventoryOwner != null;
    }

    public ClientInventorySlot getSlotAt(int index) {
        return slots[index];
    }

    public void setOwner(ClientEntity owner) {
        this.inventoryOwner = owner;
    }

    public ClientEntity getOwner() {
        return inventoryOwner;
    }

}