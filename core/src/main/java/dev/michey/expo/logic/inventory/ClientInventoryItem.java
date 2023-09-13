package dev.michey.expo.logic.inventory;

import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;

public class ClientInventoryItem {

    public int itemId;
    public int itemAmount;

    public ItemMetadata itemMetadata;

    public ClientInventoryItem(int itemId, int itemAmount) {
        this.itemId = itemId;
        this.itemAmount = itemAmount;
    }

    public ClientInventoryItem() {
        this(0, 1);
    }

    public boolean hasMetadata() {
        return itemMetadata != null;
    }

    public static ClientInventoryItem from(ServerInventoryItem item) {
        ClientInventoryItem client = new ClientInventoryItem();
        client.itemId = item.itemId;
        client.itemAmount = item.itemAmount;
        client.itemMetadata = item.itemMetadata;
        return client;
    }

    public ItemMapping toMapping() {
        return ItemMapper.get().getMapping(itemId);
    }

    public boolean isEmpty() {
        return itemId < 0;
    }

}
