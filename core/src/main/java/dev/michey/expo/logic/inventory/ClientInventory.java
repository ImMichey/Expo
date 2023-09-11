package dev.michey.expo.logic.inventory;

import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;

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

    public ClientInventory(ServerInventorySlot[] serverSlots) {
        this.slots = new ClientInventorySlot[serverSlots.length];

        for(int i = 0; i < slots.length; i++) {
            slots[i] = new ClientInventorySlot(serverSlots[i]);
        }
    }

    public void updateFrom(ServerInventorySlot[] serverSlots) {
        for(int i = 0; i < serverSlots.length; i++) {
            slots[i].item = ClientInventoryItem.from(serverSlots[i].item);
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

    public boolean hasItem(int id, int amount) {
        var slots = getSlots();
        int required = amount;

        for(ClientInventorySlot slot : slots) {
            if(slot.item == null) continue;
            if(slot.item.itemId == id) {
                required -= slot.item.itemAmount;
                if(required <= 0) return true;
            }
        }

        return false;
    }

    public ClientInventorySlot[] getSlots() {
        return slots;
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