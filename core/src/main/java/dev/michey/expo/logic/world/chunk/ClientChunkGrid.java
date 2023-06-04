package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.main.logic.world.gen.NoisePostProcessor;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;
import dev.michey.expo.util.Pair;
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
    public HashMap<String, Pair<NoisePostProcessor, Noise>> noisePostProcessorMap;

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
            noisePostProcessorMap = new HashMap<>();
        }

        INSTANCE = this;
    }

    public void applyGenSettings(WorldGenNoiseSettings noiseSettings, HashMap<BiomeType, float[]> biomeDataMap) {
        this.noiseSettings = noiseSettings;
        this.biomeDataMap = biomeDataMap;
        log("Applying world gen mapping " + noiseSettings);

        if(noiseSettings.isTerrainGenerator()) {
            noiseSettings.terrainElevation.applyTo(terrainNoiseHeight);
            noiseSettings.terrainTemperature.applyTo(terrainNoiseTemperature);
            noiseSettings.terrainMoisture.applyTo(terrainNoiseMoisture);
        }

        if(noiseSettings.isRiversGenerator()) {
            noiseSettings.river.applyTo(riverNoise);
        }

        if(noiseSettings.isPostProcessorGenerator()) {
            for(String key : noiseSettings.postProcessList.keySet()) {
                NoisePostProcessor npp = noiseSettings.postProcessList.get(key);
                Noise noise = new Noise();
                npp.noiseWrapper.applyTo(noise);
                noisePostProcessorMap.put(key, new Pair<>(npp, noise));
            }
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

    public void updateChunkData(int chunkX, int chunkY, BiomeType[] biomes, DynamicTilePart[][] individualTileData) {
        String key = chunkX + "," + chunkY;
        ClientChunk existing = clientChunkMap.get(key);

        if(existing == null) {
            clientChunkMap.put(key, new ClientChunk(chunkX, chunkY, biomes, individualTileData));
        } else {
            existing.update(biomes, individualTileData);
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
            type = convertNoise(x, y);
            noiseCacheMap.put(key, type);
        }

        return type;
    }

    private float normalized(Noise noise, int x, int y) {
        return (noise.getConfiguredNoise(x, y) + 1) * 0.5f;
    }

    private BiomeType convertNoise(int x, int y) {
        if(terrainNoiseHeight == null) return BiomeType.VOID;

        for(BiomeType toCheck : biomeDataMap.keySet()) {
            float[] values = biomeDataMap.get(toCheck);

            float elevationMin = values[0];
            float elevationMax = values[1];

            float temperatureMin = values[2];
            float temperatureMax = values[3];

            float moistureMin = values[4];
            float moistureMax = values[5];

            float height = normalized(terrainNoiseHeight, x, y);
            float temperature = normalized(terrainNoiseTemperature, x, y);
            float moisture = normalized(terrainNoiseMoisture, x, y);

            if(height >= elevationMin && height <= elevationMax && temperature >= temperatureMin && temperature <= temperatureMax && moisture >= moistureMin && moisture <= moistureMax) {
                // hook post processors
                var processor = noisePostProcessorMap.get("lakes");

                if(processor != null) {
                    float lakeValue = normalized(processor.value, x, y);
                    boolean isLake = lakeValue >= processor.key.threshold;

                    if(isLake && !BiomeType.isWater(toCheck)) {
                        float deepValue = processor.key.threshold + (1.0f - processor.key.threshold) * 0.3f;

                        if(lakeValue >= deepValue) {
                            return BiomeType.OCEAN_DEEP;
                        } else {
                            return BiomeType.LAKE;
                        }
                    }
                }

                if(toCheck != BiomeType.OCEAN_DEEP) {
                    float river = normalized(riverNoise, x, y);
                    if(river >= 0.975f) return BiomeType.RIVER;
                }

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
