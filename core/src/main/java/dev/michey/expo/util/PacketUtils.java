package dev.michey.expo.util;

import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.packet.P19_PlayerInventoryUpdate;

public class PacketUtils {

    public static void readInventoryUpdatePacket(P19_PlayerInventoryUpdate p, PlayerInventory inv) {
        for(int i = 0 ; i < p.updatedSlots.length; i++) {
            int slot = p.updatedSlots[i];
            ServerInventoryItem item = p.updatedItems[i];

            if(slot == ExpoShared.PLAYER_INVENTORY_SLOT_CURSOR) {
                if(item == null) {
                    inv.cursorItem = null;
                } else {
                    inv.cursorItem = ClientInventoryItem.from(item);
                }
            } else {
                if(item.isEmpty()) {
                    inv.getSlotAt(slot).item = null;
                } else {
                    inv.getSlotAt(slot).item = ClientInventoryItem.from(item);
                }
            }
        }
    }

}
