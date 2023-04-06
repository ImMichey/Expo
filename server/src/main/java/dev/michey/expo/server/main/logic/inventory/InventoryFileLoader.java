package dev.michey.expo.server.main.logic.inventory;

import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import org.json.JSONArray;
import org.json.JSONObject;

public class InventoryFileLoader {

    /** Loads a stored JSONArray int a ServerInventory object. */
    public static void loadFromStorage(ServerInventory inventory, JSONArray array) {
        int a = array.length();

        for(int i = 0; i < a; i++) {
            JSONObject slotObject = array.getJSONObject(i);
            int slot = slotObject.getInt("s");
            JSONObject item = slotObject.getJSONObject("i");

            loadItemFromStorage(inventory.slots[slot].item, item);
        }
    }

    /** Converts a ServerInventory object to a storage-able JSONArray. */
    public static JSONArray toStorageObject(ServerInventory inventory) {
        JSONArray array = new JSONArray();

        for(ServerInventorySlot slot : inventory.slots) {
            array.put(
                    new JSONObject()
                            .put("s", slot.slotIndex)
                            .put("i", itemToStorageObject(slot.item))
            );
        }

        return array;
    }

    public static void loadItemFromStorage(ServerInventoryItem item, JSONObject object) {
        item.itemId = object.getInt("id");

        if(object.has("am")) {
            item.itemAmount = object.getInt("am");

            if(object.has("meta")) {
                item.itemMetadata = new ItemMetadata();
                loadMetadataFromStorage(item.itemMetadata, object.getJSONObject("meta"));
            }
        }
    }

    private static void loadMetadataFromStorage(ItemMetadata metadata, JSONObject object) {
        metadata.toolType = ToolType.valueOf(object.getString("type"));
        metadata.durability = object.getInt("durability");
    }

    public static JSONObject itemToStorageObject(ServerInventoryItem item) {
        JSONObject object = new JSONObject();

        object.put("id", item.itemId);

        if(!item.isEmpty()) {
            object.put("am", item.itemAmount);

            if(item.hasMetadata()) {
                object.put("meta", metadataToStorageObject(item.itemMetadata));
            }
        }

        return object;
    }

    private static JSONObject metadataToStorageObject(ItemMetadata metadata) {
        JSONObject object = new JSONObject();

        object.put("type", metadata.toolType.name());
        object.put("durability", metadata.durability);

        return object;
    }

}
