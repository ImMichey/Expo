package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;

public class P40_InventoryView extends Packet {

    public InventoryViewType type;
    public int containerId;

    public ServerInventorySlot[] viewSlots;

}