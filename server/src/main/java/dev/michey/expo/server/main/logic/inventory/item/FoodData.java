package dev.michey.expo.server.main.logic.inventory.item;

import org.json.JSONObject;

public class FoodData {

    public float hungerRestore;
    public float hungerCooldownRestore;

    public float healthRestore;

    public FoodData(JSONObject object) {
        if(object.has("hungerRestore")) hungerRestore = object.getFloat("hungerRestore");
        if(object.has("hungerCooldownRestore")) hungerCooldownRestore = object.getFloat("hungerCooldownRestore");
        if(object.has("healthRestore")) healthRestore = object.getFloat("healthRestore");
    }

}
