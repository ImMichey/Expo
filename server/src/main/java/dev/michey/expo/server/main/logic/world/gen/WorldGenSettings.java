package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.noise.BiomeType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class WorldGenSettings {

    /** Noise */
    private WorldGenNoiseSettings noiseSettings;

    /** Biomes */
    //private final HashMap<BiomeType, float[]> biomeDataMap;
    private final LinkedList<BiomeDefinition> biomeDefinitionList;
    private final HashMap<BiomeType, List<EntityPopulator>> biomePopulatorMap;
    private final HashMap<BiomeType, List<TilePopulator>> tilePopulatorMap;

    public WorldGenSettings() {
        //biomeDataMap = new HashMap<>();
        biomeDefinitionList = new LinkedList<>();
        biomePopulatorMap = new HashMap<>();
        tilePopulatorMap = new HashMap<>();
    }

    public void parseNoiseSettings(JSONObject object) {
        noiseSettings = new WorldGenNoiseSettings();

        if(object.has("postProcessors")) {
            noiseSettings.parsePostProcessors(object.getJSONArray("postProcessors"));
        }

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

                biomeDefinitionList.add(new BiomeDefinition(BiomeType.valueOf(entry.getString("type")), values, !entry.has("priority") ? 0 : entry.getInt("priority")));
            }

            biomeDefinitionList.sort(Comparator.comparingInt(o -> o.priority));
        }
    }

    public void parseBiomeSettings(JSONObject object) {
        for(BiomeType b : BiomeType.values()) {
            if(object.has(b.name())) {
                List<EntityPopulator> list = new LinkedList<>();
                JSONArray entityArray = object.getJSONObject(b.name()).getJSONArray("entities");

                for(int i = 0; i < entityArray.length(); i++) {
                    JSONObject singlePopulatorObject = entityArray.getJSONObject(i);
                    EntityPopulator populator = new EntityPopulator(singlePopulatorObject);
                    populator.dimensionBounds = EntityPopulationBounds.get().getFor(populator.type);
                    list.add(populator);
                }

                list.sort(Comparator.comparingInt(o -> -o.priority));
                biomePopulatorMap.put(b, list);
            }
        }
    }

    public void parseTileSettings(JSONArray array) {
        for(int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            TilePopulator tilePopulator = new TilePopulator(entry);
            tilePopulator.dimensionBounds = EntityPopulationBounds.get().getFor(tilePopulator.type);

            for(BiomeType b : tilePopulator.biomes) {
                tilePopulatorMap.computeIfAbsent(b, k -> new LinkedList<>());
                tilePopulatorMap.get(b).add(tilePopulator);
            }
        }

        for(BiomeType toSort : tilePopulatorMap.keySet()) {
            tilePopulatorMap.get(toSort).sort(Comparator.comparing(o -> -o.priority));
        }
    }

    @Override
    public String toString() {
        return "WorldGenSettings{" +
                "noiseSettings=" + noiseSettings +
                ", biomeDefinitionList=" + biomeDefinitionList +
                ", biomePopulatorMap=" + biomePopulatorMap +
                ", tilePopulatorMap=" + tilePopulatorMap +
                '}';
    }

    public LinkedList<BiomeDefinition> getBiomeDefinitionList() {
        return biomeDefinitionList;
    }

    public List<EntityPopulator> getEntityPopulators(BiomeType type) {
        return biomePopulatorMap.get(type);
    }

    public List<TilePopulator> getTilePopulators(BiomeType biome) {
        return tilePopulatorMap.get(biome);
    }

    public WorldGenNoiseSettings getNoiseSettings() {
        return noiseSettings;
    }

}
