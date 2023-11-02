package dev.michey.expo.server.util;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;

public class EntityMetadata {

    private final JSONObject object;

    public EntityMetadata(JSONObject object) {
        this.object = object;
    }

    public float getMaxHealth() {
        return object.getFloat("maxHp");
    }

    public float getMaxHealth(int variant) {
        return object.getFloat("maxHp.var" + variant);
    }

    public String getName() {
        return object.getString("name");
    }

    public String getName(int variant) {
        return object.getString("name.var" + variant);
    }

    public float getFloat(String key) {
        return object.getFloat(key);
    }

    public LinkedList<ServerEntityType> getEntityTypes(String key, LinkedList<ServerEntityType> fallback) {
        if(object.has(key)) {
            LinkedList<ServerEntityType> list = new LinkedList<>();
            JSONArray parse = object.getJSONArray(key);
            for(int i = 0; i < parse.length(); i++) {
                list.add(ServerEntityType.valueOf(parse.getString(i)));
            }
            return list;
        }

        return fallback;
    }

    public int getInt(String key) {
        return object.getInt(key);
    }

    public String getString(String key) {
        return object.getString(key);
    }

}