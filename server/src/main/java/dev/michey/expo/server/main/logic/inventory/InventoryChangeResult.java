package dev.michey.expo.server.main.logic.inventory;

import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;

import java.util.LinkedList;
import java.util.List;

public class InventoryChangeResult {

    public List<Integer> changedSlots;
    public List<ServerInventoryItem> changedItems;
    public boolean changePresent;

    public void addChange(int slotId, ServerInventoryItem item) {
        if(!changePresent) {
            changedSlots = new LinkedList<>();
            changedItems = new LinkedList<>();
            changePresent = true;
        }

        changedSlots.add(slotId);
        changedItems.add(item);
    }

}
