package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class WorldGenSettings {

    /** Noise */
    private WorldGenNoiseSettings noiseSettings;

    /** Biomes */
    private final HashMap<BiomeType, float[]> biomeDataMap;
    private final HashMap<BiomeType, List<EntityPopulator>> biomePopulatorMap;

    public WorldGenSettings() {
        biomeDataMap = new HashMap<>();
        biomePopulatorMap = new HashMap<>();
    }

    public void parseNoiseSettings(JSONObject object) {
        noiseSettings = new WorldGenNoiseSettings();

        if(object.has("terrain")) {
            noiseSettings.parseTerrain(object.getJSONObject("terrain"));
        }

        if(object.has("rivers")) {
            noiseSettings.parseRivers(object.getJSONObject("rivers"));
        }

        if(object.has("biomes")) {
            JSONArray ba = object.getJSONArray("biomes");

            for(int i = 0; i < ba.length(); i++) {
                JSONObject entry = ba.getJSONObject(i);
                float[] values = new float[6];
                JSONArray e = entry.getJSONArray("elevation");
                JSONArray t = entry.getJSONArray("temperature");
                JSONArray m = entry.getJSONArray("moisture");

                values[0] = e.getFloat(0);
                values[1] = e.getFloat(1);
                values[2] = t.getFloat(0);
                values[3] = t.getFloat(1);
                values[4] = m.getFloat(0);
                values[5] = m.getFloat(1);

                biomeDataMap.put(BiomeType.valueOf(entry.getString("type")), values);
            }
        }
    }

    public void parseBiomeSettings(JSONObject object) {
        for(BiomeType b : BiomeType.values()) {
            if(object.has(b.name())) {
                List<EntityPopulator> list = new LinkedList<>();
                JSONArray entityArray = object.getJSONObject(b.name()).getJSONArray("entities");

                for(int i = 0; i < entityArray.length(); i++) {
                    JSONObject singlePopulatorObject = entityArray.getJSONObject(i);
                    list.add(new EntityPopulator(singlePopulatorObject));
                }

                biomePopulatorMap.put(b, list);
            }
        }
    }

    @Override
    public String toString() {
        return "WorldGenSettings{" +
                "noiseSettings=" + noiseSettings +
                ", biomeDataMap=" + biomeDataMap +
                ", biomePopulatorMap=" + biomePopulatorMap +
                '}';
    }

    public HashMap<BiomeType, float[]> getBiomeDataMap() {
        return biomeDataMap;
    }

    public List<EntityPopulator> getEntityPopulators(BiomeType type) {
        return biomePopulatorMap.get(type);
    }

    public WorldGenNoiseSettings getNoiseSettings() {
        return noiseSettings;
    }

}
