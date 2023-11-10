package dev.michey.expo.server.main.logic.inventory;

import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InventoryChangeResult {

    public HashMap<Integer, List<Integer>> changedSlots;
    public HashMap<Integer, List<ServerInventoryItem>> changedItems;
    public boolean changePresent;

    public void addChange(int containerId, int slotId, ServerInventoryItem item) {
        if(!changePresent) {
            changedSlots = new HashMap<>();
            changedItems = new HashMap<>();
            changePresent = true;
        }

        changedSlots.computeIfAbsent(containerId, k -> new LinkedList<>());
        changedItems.computeIfAbsent(containerId, k -> new LinkedList<>());

        changedSlots.get(containerId).add(slotId);
        changedItems.get(containerId).add(item);
    }

    public void merge(InventoryChangeResult changeResult) {
        for(int slot : changeResult.changedSlots.keySet()) {
            var a = changeResult.changedSlots.get(slot);
            var b = changeResult.changedItems.get(slot);

            for(int i : a) {
                changedSlots.get(slot).add(i);
            }

            for(ServerInventoryItem i : b) {
                changedItems.get(slot).add(i);
            }
        }
    }

}
