package dev.michey.expo.server.main.logic.inventory.item.mapping;

import dev.michey.expo.server.main.logic.inventory.item.FoodData;
import dev.michey.expo.server.main.logic.inventory.item.PlaceData;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

public class ItemLogic {

    public int maxStackSize;
    public int durability;

    public ToolType toolType; // can be null
    public FoodData foodData; // can be null
    public PlaceData placeData; // can be null

    public float range;
    public float attackSpeed;
    public float attackDamage;
    public float harvestDamage;
    public float attackAngleSpan;
    public float attackKnockbackStrength;
    public float attackKnockbackDuration;

    public ItemLogic(JSONObject object) {
        String _toolType = object.getString("toolType");

        if(!_toolType.equals("ITEM")) {
            toolType = ToolType.valueOf(_toolType);
        }

        if(object.has("stackSize")) {
            maxStackSize = object.getInt("stackSize");
        } else {
            maxStackSize = 100;
        }

        if(object.has("durability")) {
            durability = object.getInt("durability");
        } else {
            durability = -1;
        }

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

        if(object.has("attackDamage")) {
            attackDamage = object.getFloat("attackDamage");
        } else {
            attackDamage = ExpoShared.PLAYER_DEFAULT_ATTACK_DAMAGE;
        }

        if(object.has("harvestDamage")) {
            harvestDamage = object.getFloat("harvestDamage");
        } else {
            harvestDamage = ExpoShared.PLAYER_DEFAULT_HARVEST_SPEED;
        }

        if(object.has("placeData")) {
            placeData = new PlaceData(object.getJSONObject("placeData"));
        }

        if(object.has("attackAngleSpan")) {
            attackAngleSpan = object.getFloat("attackAngleSpan");
        } else {
            attackAngleSpan = ExpoShared.PLAYER_DEFAULT_ATTACK_ANGLE_SPAN;
        }

        if(object.has("attackKnockbackStrength")) {
            attackKnockbackStrength = object.getFloat("attackKnockbackStrength");
        } else {
            attackKnockbackStrength = ExpoShared.PLAYER_DEFAULT_ATTACK_KNOCKBACK_STRENGTH;
        }

        if(object.has("attackKnockbackDuration")) {
            attackKnockbackDuration = object.getFloat("attackKnockbackDuration");
        } else {
            attackKnockbackDuration = ExpoShared.PLAYER_DEFAULT_ATTACK_KNOCKBACK_DURATION;
        }
    }

    public boolean isTool() {
        return isSpecialType() && (toolType == ToolType.PICKAXE || toolType == ToolType.AXE || toolType == ToolType.SHOVEL || toolType == ToolType.SCYTHE);
    }

    public boolean isSpecialType() {
        return toolType != null;
    }

    public boolean isFood() {
        return foodData != null;
    }

    public boolean isPlaceable() {
        return placeData != null;
    }

}
