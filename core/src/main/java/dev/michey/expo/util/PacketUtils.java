package dev.michey.expo.util;

import dev.michey.expo.logic.inventory.ClientInventory;
import dev.michey.expo.logic.inventory.ClientInventoryItem;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainer;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.packet.P19_ContainerUpdate;

public class PacketUtils {

    public static void readInventoryUpdatePacket(P19_ContainerUpdate p) {
        boolean playerInv = p.containerId == ExpoShared.CONTAINER_ID_PLAYER;

        if(playerInv) {
            PlayerInventory inv = PlayerInventory.LOCAL_INVENTORY;

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
        } else {
            UIContainer container = PlayerUI.get().currentContainer;

            if(container != null) {
                ClientInventory inv = PlayerUI.get().currentContainer.clientInventory;

                for(int i = 0; i < p.updatedSlots.length; i++) {
                    int slot = p.updatedSlots[i];
                    ServerInventoryItem item = p.updatedItems[i];

                    if(item.isEmpty()) {
                        inv.getSlotAt(slot).item = null;
                    } else {
                        inv.getSlotAt(slot).item = ClientInventoryItem.from(item);
                    }
                }
            }
        }
    }

}
