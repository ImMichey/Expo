package dev.michey.expo.server.main.logic.world.spawn;

import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;

public class EntitySpawnDatabase {

    private static EntitySpawnDatabase INSTANCE;
    private final HashMap<String, EntitySpawner> spawnerMap;

    public EntitySpawnDatabase() {
        spawnerMap = new HashMap<>();

        JSONArray database = JsonConverter.fileToJson("entity_spawns.json").getJSONArray("database");

        for(int i = 0; i < database.length(); i++) {
            JSONObject o = database.getJSONObject(i);
            spawnerMap.put(o.getString("identifier"), new EntitySpawner(o));
        }
    }

    public LinkedList<EntitySpawner> getFor(String dimensionName) {
        LinkedList<EntitySpawner> list = new LinkedList<>();

        for(EntitySpawner all : spawnerMap.values()) {
            for(String dim : all.dimensions) {
                if(dim.equals(dimensionName)) {
                    list.add(all);
                    break;
                }
            }
        }

        return list;
    }

    public static EntitySpawnDatabase get() {
        if(INSTANCE == null) INSTANCE = new EntitySpawnDatabase();
        return INSTANCE;
    }

}
