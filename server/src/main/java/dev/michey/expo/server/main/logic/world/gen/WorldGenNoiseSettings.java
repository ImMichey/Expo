package dev.michey.expo.server.main.logic.world.gen;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class WorldGenNoiseSettings {

    /** Noise TERRAIN */
    public NoiseWrapper terrainElevation;
    public NoiseWrapper terrainTemperature;
    public NoiseWrapper terrainMoisture;

    /** Noise RIVERS */
    public NoiseWrapper river;

    /** Noise POST PROCESS */
    public HashMap<String, NoisePostProcessor> postProcessList;

    public boolean isTerrainGenerator() {
        return terrainElevation != null;
    }

    public boolean isRiversGenerator() {
        return river != null;
    }

    public boolean isPostProcessorGenerator() {
        return postProcessList != null;
    }

    public void parsePostProcessors(JSONArray array) {
        if(array.length() == 0) return;
        postProcessList = new HashMap<>();

        for(int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);

            postProcessList.put(o.getString("name"), new NoisePostProcessor(new NoiseWrapper(o.getString("name"),
                    o.getInt("octaves"),
                    o.getInt("type"),
                    o.has("fractalType") ? o.getInt("fractalType") : -1,
                    o.getFloat("frequency")), o.getFloat("level")));
        }
    }

    public void parseTerrain(JSONObject o) {
        {
            JSONObject elevation = o.getJSONObject("elevation");
            terrainElevation = new NoiseWrapper("terrainElevation",
                    elevation.getInt("octaves"),
                    elevation.getInt("type"),
                    elevation.has("fractalType") ? elevation.getInt("fractalType") : -1,
                    elevation.getFloat("frequency"));
        }

        {
            JSONObject temperature = o.getJSONObject("temperature");
            terrainTemperature = new NoiseWrapper("terrainElevation",
                    temperature.getInt("octaves"),
                    temperature.getInt("type"),
                    temperature.has("fractalType") ? temperature.getInt("fractalType") : -1,
                    temperature.getFloat("frequency"));
        }

        {
            JSONObject moisture = o.getJSONObject("moisture");
            terrainMoisture = new NoiseWrapper("terrainElevation",
                    moisture.getInt("octaves"),
                    moisture.getInt("type"),
                    moisture.has("fractalType") ? moisture.getInt("fractalType") : -1,
                    moisture.getFloat("frequency"));
        }
    }

    public void parseRivers(JSONObject o) {
        river = new NoiseWrapper("terrainElevation",
                o.getInt("octaves"),
                o.getInt("type"),
                o.has("fractalType") ? o.getInt("fractalType") : -1,
                o.getFloat("frequency"));
    }

    @Override
    public String toString() {
        return "WorldGenNoiseSettings{" +
                "terrainElevation=" + terrainElevation +
                ", terrainTemperature=" + terrainTemperature +
                ", terrainMoisture=" + terrainMoisture +
                ", river=" + river +
                ", postProcessList=" + postProcessList +
                '}';
    }

}
