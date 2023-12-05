package dev.michey.expo.server.main.logic.world.gen;

import dev.michey.expo.server.util.JsonConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.LinkedList;

public class WorldGenNoiseSettings {

    /** Noise TERRAIN */
    public NoiseWrapper terrainElevation;
    public NoiseWrapper terrainTemperature;
    public NoiseWrapper terrainMoisture;

    /** Noise RIVERS */
    public NoiseWrapper river;

    /** Noise POST PROCESS */
    //public HashMap<String, NoisePostProcessor> postProcessList;
    public LinkedList<NoisePostProcessor> postProcessList;

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
        if(array.isEmpty()) return;
        postProcessList = new LinkedList<>();

        for(int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);

            JSONObject logicObject = o.getJSONObject("logic");

            String _type = logicObject.getString("replaceType").toUpperCase();
            String[] _logicKeys = JsonConverter.pullStrings(logicObject.getJSONArray("check"));
            String _replaceWith = logicObject.getString("replaceWith");
            float _secondOptionThreshold = logicObject.has("thresholdSecond") ? logicObject.getFloat("thresholdSecond") : -1.0f;
            String _secondOptionReplace = logicObject.has("thresholdReplace") ? logicObject.getString("thresholdReplace") : null;
            String _typeSecond = logicObject.has("replaceTypeSecond") ? logicObject.getString("replaceTypeSecond").toUpperCase() : _type;
            float _thresholdA;
            float _thresholdB;

            if(logicObject.has("thresholdA") && logicObject.has("thresholdB")) {
                _thresholdA = logicObject.getFloat("thresholdA");
                _thresholdB = logicObject.getFloat("thresholdB");
            } else {
                _thresholdA = logicObject.getFloat("threshold");
                _thresholdB = 1.0f;
            }

            int priority = logicObject.has("priority") ? logicObject.getInt("priority") : 0;

            PostProcessorLogic logic;

            if(_type.equals("BIOME")) {
                logic = new PostProcessorBiome(o.getString("name"), _thresholdA, _thresholdB, _logicKeys, _replaceWith, _secondOptionThreshold, _secondOptionReplace, null);
            } else {
                logic = new PostProcessorLayer(o.getString("name"), _thresholdA, _thresholdB, _logicKeys, _type, _replaceWith, _secondOptionThreshold, _secondOptionReplace, _typeSecond);
            }

            postProcessList.add(new NoisePostProcessor(priority, new NoiseWrapper(o.getString("name"),
                    o.getInt("octaves"),
                    o.getInt("type"),
                    o.has("fractalType") ? o.getInt("fractalType") : -1,
                    o.getFloat("frequency"),
                    o.getInt("noiseOffset")), logic));
        }

        postProcessList.sort(Comparator.comparingInt(o -> -o.priority));
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
