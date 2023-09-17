package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONObject;

import java.util.Arrays;

public class TilePopulator {

    public BiomeType biome;
    public ServerEntityType type;
    public float chance;
    public float[] spawnOffsets;
    public boolean asStaticEntity;
    public EntityBoundsEntry dimensionBounds = null;
    public SpreadData spreadData;
    public int priority;
    public int skip = 1;

    public TilePopulator(JSONObject entry) {
        biome = BiomeType.valueOf(entry.getString("biome"));
        type = ServerEntityType.valueOf(entry.getString("type"));
        chance = entry.getFloat("chance");
        asStaticEntity = entry.getBoolean("static");
        spawnOffsets = JsonConverter.pullFloats(entry.getJSONArray("spawnOffsets"));
        if(entry.has("priority")) priority = entry.getInt("priority");
        if(entry.has("skip")) skip = entry.getInt("skip");

        // optional
        if(entry.has("spreadData")) {
            JSONObject spread = entry.getJSONObject("spreadData");
            spreadData = new SpreadData(spread);
        }
    }

    @Override
    public String toString() {
        return "TilePopulator{" +
                "biome=" + biome +
                ", type=" + type +
                ", chance=" + chance +
                ", spawnOffsets=" + Arrays.toString(spawnOffsets) +
                ", asStaticEntity=" + asStaticEntity +
                ", dimensionBounds=" + dimensionBounds +
                ", spreadData=" + spreadData +
                ", priority=" + priority +
                ", skip=" + skip +
                '}';
    }

}
