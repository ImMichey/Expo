package dev.michey.expo.server.util;

import org.json.JSONObject;

public class EntityMetadata {

    private final JSONObject object;

    public EntityMetadata(JSONObject object) {
        this.object = object;
    }

    public float getMaxHealth() {
        return object.getFloat("maxHp");
    }

    public float getFloat(String key) {
        return object.getFloat(key);
    }

    public int getInt(String key) {
        return object.getInt(key);
    }

    public String getString(String key) {
        return object.getString(key);
    }

}