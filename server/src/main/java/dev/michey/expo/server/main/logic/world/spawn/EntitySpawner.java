package dev.michey.expo.server.main.logic.world.spawn;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONObject;

public class EntitySpawner {

    public String[] dimensions;
    public String identifier;
    public int[] spawnTimeframes;
    public ServerEntityType spawnType;
    public BiomeType[] spawnBiomes;
    public int spawnMin, spawnMax;
    public float spawnChance;
    public float spawnTimer;
    public float playerDistanceMin;
    public int entityCapPerPlayer;

    public EntitySpawner(JSONObject json) {
        identifier = json.getString("identifier");
        dimensions = JsonConverter.pullStrings(json.getJSONArray("dimensions"));
        spawnTimeframes = JsonConverter.pullInts(json.getJSONArray("timeframes"));
        spawnType = ServerEntityType.valueOf(json.getString("type"));
        spawnBiomes = JsonConverter.pullBiomes(json.getJSONArray("biomes"));
        spawnMin = json.getInt("amountMin");
        spawnMax = json.getInt("amountMax");
        spawnChance = json.getFloat("chance");
        spawnTimer = json.getFloat("timer");
        playerDistanceMin = json.has("playerDistanceMin") ? json.getFloat("playerDistanceMin") : -1;
        entityCapPerPlayer = json.has("entityCapPerPlayer") ? json.getInt("entityCapPerPlayer") : -1;
    }

}
