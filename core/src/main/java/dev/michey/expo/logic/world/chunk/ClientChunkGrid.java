package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;
import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;
import make.some.noise.Noise;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientChunkGrid {

    private static ClientChunkGrid INSTANCE;

    private final ConcurrentHashMap<String, ClientChunk> clientChunkMap;

    /** Minimap only noise logic on dedicated server connection. */
    public Noise terrainNoiseHeight;
    public Noise terrainNoiseTemperature;
    public Noise terrainNoiseMoisture;
    public Noise riverNoise;
    public WorldGenNoiseSettings noiseSettings;
    public HashMap<BiomeType, float[]> biomeDataMap;

    private HashMap<String, BiomeType> noiseCacheMap;

    /** Water wave logic */
    private float waveDelta;
    private float waveCooldown;
    private float factor = 1.0f;
    public float interpolation;

    public ClientChunkGrid() {
        clientChunkMap = new ConcurrentHashMap<>();

        if(ExpoServerLocal.get() == null) {
            terrainNoiseHeight = new Noise();
            terrainNoiseTemperature = new Noise();
            terrainNoiseMoisture = new Noise();
            riverNoise = new Noise();
            noiseCacheMap = new HashMap<>();
        }

        INSTANCE = this;
    }

    public void applyGenSettings(WorldGenNoiseSettings noiseSettings, HashMap<BiomeType, float[]> biomeDataMap) {
        this.noiseSettings = noiseSettings;
        this.biomeDataMap = biomeDataMap;
        log("Applying world gen mapping " + noiseSettings);

        if(noiseSettings.isTerrainGenerator()) {
            terrainNoiseHeight.setFrequency(noiseSettings.terrainElevationFrequency);
            terrainNoiseHeight.setNoiseType(noiseSettings.terrainElevationType);
            terrainNoiseHeight.setFractalOctaves(noiseSettings.terrainElevationOctaves);
            if(noiseSettings.terrainElevationFractalType != -1) terrainNoiseHeight.setFractalType(noiseSettings.terrainElevationFractalType);

            terrainNoiseTemperature.setFrequency(noiseSettings.terrainTemperatureFrequency);
            terrainNoiseTemperature.setNoiseType(noiseSettings.terrainTemperatureType);
            terrainNoiseTemperature.setFractalOctaves(noiseSettings.terrainTemperatureOctaves);
            if(noiseSettings.terrainTemperatureFractalType != -1) terrainNoiseTemperature.setFractalType(noiseSettings.terrainTemperatureFractalType);

            terrainNoiseMoisture.setFrequency(noiseSettings.terrainMoistureFrequency);
            terrainNoiseMoisture.setNoiseType(noiseSettings.terrainMoistureType);
            terrainNoiseMoisture.setFractalOctaves(noiseSettings.terrainMoistureOctaves);
            if(noiseSettings.terrainMoistureFractalType != -1) terrainNoiseMoisture.setFractalType(noiseSettings.terrainMoistureFractalType);
        }

        if(noiseSettings.isRiversGenerator()) {
            riverNoise.setFrequency(noiseSettings.noiseRiversFrequency);
            riverNoise.setNoiseType(noiseSettings.noiseRiversType);
            riverNoise.setFractalOctaves(noiseSettings.noiseRiversOctaves);
            if(noiseSettings.noiseRiversFractalType != -1) riverNoise.setFractalType(noiseSettings.noiseRiversFractalType);
        }
    }

    public void tick(float delta) {
        float WAVE_SPEED = 0.75f;
        float WAVE_COOLDOWN_IN = 0.1f;
        float WAVE_COOLDOWN_OUT = 0.8f;
        float WAVE_STRENGTH = 2.0f;

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
        String key = chunkX + "," + chunkY;
        ClientChunk existing = clientChunkMap.get(key);

        if(existing == null) {
            clientChunkMap.put(key, new ClientChunk(chunkX, chunkY, biomes, layer0, layer1, layer2));
        } else {
            existing.update(biomes, layer0, layer1, layer2);
        }
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(int x, int y) {
        String key = x + "," + y;
        return getBiome(x, y, key);
    }

    public BiomeType getBiome(int x, int y, String key) {
        BiomeType type = noiseCacheMap.get(key);

        if(type == null) {
            float normalizedHeight = noiseSettings.isTerrainGenerator() ? ((terrainNoiseHeight.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedTemperature = noiseSettings.isTerrainGenerator() ? ((terrainNoiseTemperature.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedMoisture = noiseSettings.isTerrainGenerator() ? ((terrainNoiseMoisture.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedRiver = noiseSettings.isRiversGenerator() ? ((riverNoise.getConfiguredNoise(x, y) + 1) / 2f) : -1;

            type = convertNoise(normalizedHeight, normalizedTemperature, normalizedMoisture, normalizedRiver);
            noiseCacheMap.put(key, type);
        }

        return type;
    }

    private BiomeType convertNoise(float height, float temperature, float moisture, float river) {
        if(height == -1) return BiomeType.VOID;
        // riverNoise is ignored for now.

        for(BiomeType toCheck : biomeDataMap.keySet()) {
            float[] values = biomeDataMap.get(toCheck);

            float elevationMin = values[0];
            float elevationMax = values[1];

            float temperatureMin = values[2];
            float temperatureMax = values[3];

            float moistureMin = values[4];
            float moistureMax = values[5];

            if(height >= elevationMin && height <= elevationMax
                    && temperature >= temperatureMin && temperature <= temperatureMax
                    && moisture >= moistureMin && moisture <= moistureMax) {
                return toCheck;
            }
        }

        return BiomeType.VOID;
    }

    public ClientChunk getChunk(int x, int y) {
        return clientChunkMap.get(x + "," + y);
    }

    public static ClientChunkGrid get() {
        return INSTANCE;
    }

}
