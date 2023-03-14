package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.math.Interpolation;
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

    /** Water wave logic */
    private float waveDelta;
    private float waveCooldown;
    private float factor = 1.0f;
    public float interpolation;
    private float WAVE_SPEED = 0.75f;
    private float WAVE_COOLDOWN_IN = 0.1f;
    private float WAVE_COOLDOWN_OUT = 0.8f;
    private float WAVE_STRENGTH = 2.0f;

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

    public void tick(float delta) {
        if(waveCooldown > 0) {
            waveCooldown -= delta;
        } else {
            waveDelta += delta * factor * WAVE_SPEED;

            if(waveDelta >= 1.0f) {
                waveDelta = 1.0f;
                factor *= -1;
                waveCooldown = WAVE_COOLDOWN_IN;
            } else if(waveDelta < 0) {
                waveDelta = 0;
                factor *= -1;
                waveCooldown = WAVE_COOLDOWN_OUT;
            }
        }

        interpolation = Interpolation.smooth2.apply(waveDelta) * WAVE_STRENGTH;
    }

    public void updateChunkData(int chunkX, int chunkY, BiomeType[] biomes, int[][] layer0, int[][] layer1, int[][] layer2) {
        //log("updateChunkData " + chunkX + " " + chunkY);
        String key = chunkX + "," + chunkY;
        clientChunkMap.put(key, new ClientChunk(chunkX, chunkY, biomes, layer0, layer1, layer2));
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
