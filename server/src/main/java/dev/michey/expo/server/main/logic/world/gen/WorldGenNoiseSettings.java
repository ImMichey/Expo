package dev.michey.expo.server.main.logic.world.gen;

import org.json.JSONObject;

public class WorldGenNoiseSettings {

    /** Noise TERRAIN */
    public int terrainElevationOctaves;
    public int terrainElevationType;
    public int terrainElevationFractalType = -1;
    public float terrainElevationFrequency;

    public int terrainTemperatureOctaves;
    public int terrainTemperatureType;
    public int terrainTemperatureFractalType = -1;
    public float terrainTemperatureFrequency;

    public int terrainMoistureOctaves;
    public int terrainMoistureType;
    public int terrainMoistureFractalType = -1;
    public float terrainMoistureFrequency;

    private boolean terrainElevation;

    /** Noise RIVERS */
    public int noiseRiversOctaves;
    public int noiseRiversType;
    public int noiseRiversFractalType = -1;
    public float noiseRiversFrequency;
    private boolean noiseRivers;

    public boolean isTerrainGenerator() {
        return terrainElevation;
    }

    public boolean isRiversGenerator() {
        return noiseRivers;
    }

    public void parseTerrain(JSONObject o) {
        terrainElevation = true;

        {
            JSONObject elevation = o.getJSONObject("elevation");
            terrainElevationOctaves = elevation.getInt("octaves");
            terrainElevationType = elevation.getInt("type");
            terrainElevationFrequency = elevation.getFloat("frequency");
            if(elevation.has("fractalType")) terrainElevationFractalType = elevation.getInt("fractalType");
        }

        {
            JSONObject temperature = o.getJSONObject("temperature");
            terrainTemperatureOctaves = temperature.getInt("octaves");
            terrainTemperatureType = temperature.getInt("type");
            terrainTemperatureFrequency = temperature.getFloat("frequency");
            if(temperature.has("fractalType")) terrainTemperatureFractalType = temperature.getInt("fractalType");
        }

        {
            JSONObject moisture = o.getJSONObject("moisture");
            terrainMoistureOctaves = moisture.getInt("octaves");
            terrainMoistureType = moisture.getInt("type");
            terrainMoistureFrequency = moisture.getFloat("frequency");
            if(moisture.has("fractalType")) terrainMoistureFractalType = moisture.getInt("fractalType");
        }
    }

    public void parseRivers(JSONObject o) {
        noiseRivers = true;
        noiseRiversOctaves = o.getInt("octaves");
        noiseRiversType = o.getInt("type");
        noiseRiversFrequency = o.getFloat("frequency");
        if(o.has("fractalType")) noiseRiversFractalType = o.getInt("fractalType");
    }

}
