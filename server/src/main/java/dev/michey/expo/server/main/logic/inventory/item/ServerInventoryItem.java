package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemLogic;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;

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

    public void setEmpty() {
        itemId = -1;
        itemAmount = 0;
        itemMetadata = null;
    }

    public ServerInventoryItem() {

    }

    public ServerInventoryItem(int itemId, int itemAmount) {
        this.itemId = itemId;
        this.itemAmount = itemAmount;

        ItemLogic logic = ItemMapper.get().getMapping(itemId).logic;

        if(logic.isSpecialType()) {
            itemMetadata = new ItemMetadata();
            itemMetadata.toolType = logic.toolType;
            itemMetadata.durability = logic.durability;
        }
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

    public ToolType isTool(ToolType... types) {
        if(itemMetadata == null) return null;
        for(ToolType t : types) {
            if(itemMetadata.toolType == t) return t;
        }
        return null;
    }

    public boolean isTool(ToolType type) {
        if(itemMetadata == null) return false;
        return itemMetadata.toolType == type;
    }

    public ItemMapping toMapping() {
        return ItemMapper.get().getMapping(itemId);
    }

}
