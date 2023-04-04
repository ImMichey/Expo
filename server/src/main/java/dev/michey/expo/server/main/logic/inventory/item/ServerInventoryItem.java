package dev.michey.expo.server.main.logic.inventory.item;

public class ServerInventoryItem {

    public int itemId = -1;
    public int itemAmount;

    public ItemMetadata itemMetadata;

    public boolean hasMetadata() {
        return itemMetadata != null;
    }

    public boolean isEmpty() {
        return itemId == -1;
    }

    public ServerInventoryItem() {

    }

    public ServerInventoryItem(int itemId, int itemAmount) {
        this.itemId = itemId;
        this.itemAmount = itemAmount;
    }

    public ServerInventoryItem clone(ServerInventoryItem from) {
        return clone(from, -1);
    }

    public ServerInventoryItem clone(ServerInventoryItem from, int amount) {
        itemId = from.itemId;
        itemAmount = amount == -1 ? from.itemAmount : amount;

        if(from.hasMetadata()) {
            itemMetadata = new ItemMetadata();
            itemMetadata.durability = from.itemMetadata.durability;
            itemMetadata.toolType = from.itemMetadata.toolType;
        }

        return this;
    }

}
