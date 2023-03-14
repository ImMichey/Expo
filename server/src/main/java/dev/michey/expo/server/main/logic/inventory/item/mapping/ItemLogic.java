package dev.michey.expo.server.main.logic.inventory.item.mapping;

import dev.michey.expo.server.main.logic.inventory.item.FoodData;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

public class ItemLogic {

    public int maxStackSize;
    public ToolType toolType; // can be null
    public int durability;
    public FoodData foodData; // can be null
    public float range;
    public float attackSpeed;

    public ItemLogic(JSONObject object) {
        maxStackSize = object.getInt("stackSize");
        String _toolType = object.getString("toolType");

        if(!_toolType.equals("ITEM")) {
            toolType = ToolType.valueOf(_toolType);
        }

        durability = object.getInt("durability");

        if(object.has("foodData")) {
            foodData = new FoodData(object.getJSONObject("foodData"));
        }

        if(object.has("range")) {
            range = object.getFloat("range");
        } else {
            range = ExpoShared.PLAYER_DEFAULT_RANGE;
        }

        if(object.has("attackSpeed")) {
            attackSpeed = object.getFloat("attackSpeed");
        } else {
            attackSpeed = ExpoShared.PLAYER_DEFAULT_ATTACK_SPEED;
        }
    }

    public boolean isTool() {
        return toolType != null;
    }

    public boolean isFood() {
        return foodData != null;
    }

}
