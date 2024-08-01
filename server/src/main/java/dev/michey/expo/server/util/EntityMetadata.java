package dev.michey.expo.server.util;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.gen.EntityBoundsEntry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;

public class EntityMetadata {

    private final JSONObject object;
    private final EntityBoundsEntry populationBbox;
    private final EntityBoundsEntry placeBbox;

    public EntityMetadata(JSONObject object) {
        this.object = object;

        if(object.has("bbox.population")) {
            populationBbox = new EntityBoundsEntry(object.getJSONArray("bbox.population"));
        } else {
            populationBbox = null;
        }

        if(object.has("bbox.place")) {
            placeBbox = new EntityBoundsEntry(object.getJSONArray("bbox.place"));
        } else {
            placeBbox = null;
        }
    }

    public EntityBoundsEntry getPlaceBbox() {
        return placeBbox;
    }

    public EntityBoundsEntry getPopulationBbox() {
        return populationBbox;
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

    public float getHealthBarOffsetY() {
        if(object.has("healthBar.offsetY")) {
            return getFloat("healthBar.offsetY");
        }
        return 0;
    }

    public float getAttackOffsetY() {
        if(object.has("ai.attackOffsetY")) {
            return getFloat("ai.attackOffsetY");
        }
        return 0;
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