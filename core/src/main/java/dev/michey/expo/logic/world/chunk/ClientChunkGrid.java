package dev.michey.expo.logic.world.chunk;

import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.noise.BiomeType;
import make.some.noise.Noise;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientChunkGrid {

    private static ClientChunkGrid INSTANCE;

    private ConcurrentHashMap<String, ClientChunk> clientChunkMap;

    /** Minimap only noise logic on dedicated server connection. */
    public Noise noise;
    public Noise riverNoise;
    private HashMap<String, BiomeType> noiseCacheMap;

    public ClientChunkGrid() {
        clientChunkMap = new ConcurrentHashMap<>();

        if(ExpoServerLocal.get() == null) {
            noise = new Noise(0, 1f/80f, Noise.FOAM_FRACTAL, 5); // 1f/384f + 5
            riverNoise = new Noise(0, 1f/384f, Noise.SIMPLEX_FRACTAL, 1); // 1f/384f + 1
            riverNoise.setFractalType(Noise.RIDGED_MULTI);
            noiseCacheMap = new HashMap<>();
        }

        INSTANCE = this;
    }

    public void updateChunkData(int chunkX, int chunkY, BiomeType[] biomeData, int[] tileIndexData, boolean[] waterLoggedData) {
        //log("updateChunkData " + chunkX + " " + chunkY);
        String key = chunkX + "," + chunkY;
        clientChunkMap.put(key, new ClientChunk(chunkX, chunkY, biomeData, tileIndexData, waterLoggedData));
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(int x, int y) {
        String key = x + "," + y;
        return getBiome(x, y, key);
    }

    public BiomeType getBiome(int x, int y, String key) {
        BiomeType type = noiseCacheMap.get(key);

        if(type == null) {
            float normalizedNoise = (noise.getConfiguredNoise(x, y) + 1) / 2f;
            float normalizedRiver = (riverNoise.getConfiguredNoise(x, y) + 1) / 2f;

            type = BiomeType.convertNoise(normalizedNoise, normalizedRiver);
            noiseCacheMap.put(key, type);
        }

        return type;
    }

    public ClientChunk getChunk(int x, int y) {
        return clientChunkMap.get(x + "," + y);
    }

    public static ClientChunkGrid get() {
        return INSTANCE;
    }

}
