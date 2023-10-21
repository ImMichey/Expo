package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class TilePopulator {

    public BiomeType[] biomes;
    public ServerEntityType type;
    public float chance;
    public float[] spawnOffsets;
    public boolean asStaticEntity;
    public EntityBoundsEntry dimensionBounds = null;
    public SpreadData spreadData;
    public int priority;
    public int skip = 1;
    public BorderRequirement borderRequirement = null;

    public TilePopulator(JSONObject entry) {
        if(entry.get("biome") instanceof String s) {
            biomes = new BiomeType[] {BiomeType.valueOf(s)};
        } else {
            JSONArray arrayOfBiomes = entry.getJSONArray("biome");
            biomes = new BiomeType[arrayOfBiomes.length()];
            for(int i = 0; i < arrayOfBiomes.length(); i++) {
                biomes[i] = BiomeType.valueOf(arrayOfBiomes.getString(i));
            }
        }

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

        if(entry.has("borderRequirement")) {
            borderRequirement = BorderRequirement.valueOf(entry.getString("borderRequirement"));
        }
    }

    @Override
    public String toString() {
        return "TilePopulator{" +
                "biomes=" + Arrays.toString(biomes) +
                ", type=" + type +
                ", chance=" + chance +
                ", spawnOffsets=" + Arrays.toString(spawnOffsets) +
                ", asStaticEntity=" + asStaticEntity +
                ", dimensionBounds=" + dimensionBounds +
                ", spreadData=" + spreadData +
                ", priority=" + priority +
                ", skip=" + skip +
                ", borderRequirement=" + borderRequirement +
                '}';
    }

}