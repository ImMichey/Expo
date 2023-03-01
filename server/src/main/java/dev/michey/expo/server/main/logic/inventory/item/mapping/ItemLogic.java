package dev.michey.expo.server.main.logic.inventory.item.mapping;

import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import org.json.JSONObject;

public class ItemLogic {

    public int maxStackSize;
    public ToolType toolType; // can be null
    public int durability;

    public ItemLogic(JSONObject object) {
        maxStackSize = object.getInt("stackSize");
        String _toolType = object.getString("toolType");

        if(!_toolType.equals("ITEM")) {
            toolType = ToolType.valueOf(_toolType);
        }

        durability = object.getInt("durability");
    }

}
