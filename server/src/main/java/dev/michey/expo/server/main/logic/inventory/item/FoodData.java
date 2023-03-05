package dev.michey.expo.server.main.logic.inventory.item;

import org.json.JSONObject;

public class FoodData {

    public float hungerRestore;
    public float hungerCooldownRestore;

    public FoodData(JSONObject object) {
        hungerRestore = object.getFloat("hungerRestore");
        hungerCooldownRestore = object.getFloat("hungerCooldownRestore");
    }

}
