package dev.michey.expo.server.main.logic.world.spawn;

import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

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

    public HashMap<String, EntitySpawner> getSpawnerMap() {
        return spawnerMap;
    }

    public static EntitySpawnDatabase get() {
        if(INSTANCE == null) INSTANCE = new EntitySpawnDatabase();
        return INSTANCE;
    }

}
