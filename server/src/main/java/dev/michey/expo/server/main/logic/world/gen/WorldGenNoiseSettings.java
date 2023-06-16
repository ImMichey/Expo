package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.util.JsonConverter;
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

            JSONObject logicObject = o.getJSONObject("logic");

            String _type = logicObject.getString("replaceType").toUpperCase();
            String[] _logicKeys = JsonConverter.pullStrings(logicObject.getJSONArray("check"));
            String _replaceWith = logicObject.getString("replaceWith");
            float _secondOptionThreshold = logicObject.has("thresholdSecond") ? logicObject.getFloat("thresholdSecond") : -1.0f;
            String _secondOptionReplace = logicObject.has("thresholdReplace") ? logicObject.getString("thresholdReplace") : null;
            float _threshold = logicObject.getFloat("threshold");

            PostProcessorLogic logic;

            if(_type.equals("BIOME")) {
                logic = new PostProcessorBiome(o.getString("name"), _threshold, _logicKeys, _replaceWith, _secondOptionThreshold, _secondOptionReplace);
            } else {
                logic = new PostProcessorLayer(o.getString("name"), _threshold, _logicKeys, _type, _replaceWith, _secondOptionThreshold, _secondOptionReplace);
            }

            postProcessList.put(o.getString("name"), new NoisePostProcessor(new NoiseWrapper(o.getString("name"),
                    o.getInt("octaves"),
                    o.getInt("type"),
                    o.has("fractalType") ? o.getInt("fractalType") : -1,
                    o.getFloat("frequency"),
                    o.getInt("noiseOffset")), logic));
        }
    }

    public void parseTerrain(JSONObject o) {
        {
            JSONObject elevation = o.getJSONObject("elevation");
            terrainElevation = new NoiseWrapper("terrainElevation",
                    elevation.getInt("octaves"),
                    elevation.getInt("type"),
                    elevation.has("fractalType") ? elevation.getInt("fractalType") : -1,
                    elevation.getFloat("frequency"),
                    o.has("noiseOffset") ? o.getInt("noiseOffset") : 0);
        }

        {
            JSONObject temperature = o.getJSONObject("temperature");
            terrainTemperature = new NoiseWrapper("terrainElevation",
                    temperature.getInt("octaves"),
                    temperature.getInt("type"),
                    temperature.has("fractalType") ? temperature.getInt("fractalType") : -1,
                    temperature.getFloat("frequency"),
                    o.has("noiseOffset") ? o.getInt("noiseOffset") : 0);
        }

        {
            JSONObject moisture = o.getJSONObject("moisture");
            terrainMoisture = new NoiseWrapper("terrainElevation",
                    moisture.getInt("octaves"),
                    moisture.getInt("type"),
                    moisture.has("fractalType") ? moisture.getInt("fractalType") : -1,
                    moisture.getFloat("frequency"),
                    o.has("noiseOffset") ? o.getInt("noiseOffset") : 0);
        }
    }

    public void parseRivers(JSONObject o) {
        river = new NoiseWrapper("terrainElevation",
                o.getInt("octaves"),
                o.getInt("type"),
                o.has("fractalType") ? o.getInt("fractalType") : -1,
                o.getFloat("frequency"),
                o.has("noiseOffset") ? o.getInt("noiseOffset") : 0);
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
