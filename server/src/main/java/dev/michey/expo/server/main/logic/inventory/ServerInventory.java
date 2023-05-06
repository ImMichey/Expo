package dev.michey.expo.server.main.logic.inventory;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.util.Pair;

import java.util.LinkedList;
import java.util.List;

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

    public Pair<Boolean, List<Pair<Integer, Integer>>> containsItem(int id, int amount) {
        Pair<Boolean, List<Pair<Integer, Integer>>> pair = new Pair<>(true, null);
        int required = amount;

        for(ServerInventorySlot slot : slots) {
            if(slot.item.itemId == id) {
                required -= slot.item.itemAmount;
                if(pair.value == null) pair.value = new LinkedList<>();
                pair.value.add(new Pair<>(slot.slotIndex, slot.item.itemAmount));
                if(required <= 0) return pair;
            }
        }

        pair.key = false;
        return pair;
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