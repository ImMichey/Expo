package dev.michey.expo.server.main.logic.world.gen;

import org.json.JSONObject;

public class WorldGenNoiseSettings {

    /** Noise TERRAIN */
    public int noiseTerrainOctaves;
    public int noiseTerrainType;
    public int noiseTerrainFractalType = -1;
    public float noiseTerrainFrequency;
    private boolean noiseTerrain;

    /** Noise RIVERS */
    public int noiseRiversOctaves;
    public int noiseRiversType;
    public int noiseRiversFractalType = -1;
    public float noiseRiversFrequency;
    private boolean noiseRivers;

    public boolean isTerrainGenerator() {
        return noiseTerrain;
    }

    public boolean isRiversGenerator() {
        return noiseRivers;
    }

    public void parseTerrain(JSONObject o) {
        noiseTerrain = true;
        noiseTerrainOctaves = o.getInt("octaves");
        noiseTerrainType = o.getInt("type");
        noiseTerrainFrequency = o.getFloat("frequency");
        if(o.has("fractalType")) {
            noiseTerrainFractalType = o.getInt("fractalType");
        }
    }

    public void parseRivers(JSONObject o) {
        noiseRivers = true;
        noiseRiversOctaves = o.getInt("octaves");
        noiseRiversType = o.getInt("type");
        noiseRiversFrequency = o.getFloat("frequency");
        if(o.has("fractalType")) {
            noiseRiversFractalType = o.getInt("fractalType");
        }
    }

    @Override
    public String toString() {
        return "WorldGenNoiseSettings{" +
                "noiseTerrainOctaves=" + noiseTerrainOctaves +
                ", noiseTerrainType=" + noiseTerrainType +
                ", noiseTerrainFractalType=" + noiseTerrainFractalType +
                ", noiseTerrainFrequency=" + noiseTerrainFrequency +
                ", noiseTerrain=" + noiseTerrain +
                ", noiseRiversOctaves=" + noiseRiversOctaves +
                ", noiseRiversType=" + noiseRiversType +
                ", noiseRiversFractalType=" + noiseRiversFractalType +
                ", noiseRiversFrequency=" + noiseRiversFrequency +
                ", noiseRivers=" + noiseRivers +
                '}';
    }

}
