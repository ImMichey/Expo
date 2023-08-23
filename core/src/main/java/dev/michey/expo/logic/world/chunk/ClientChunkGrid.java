package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.main.logic.world.gen.NoisePostProcessor;
import dev.michey.expo.server.main.logic.world.gen.PostProcessorBiome;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;
import dev.michey.expo.server.packet.P11_ChunkData;
import dev.michey.expo.util.Pair;
import make.some.noise.Noise;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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

    private HashMap<String, Pair<BiomeType, Float>> noiseCacheMap;
    public LinkedList<Pair<NoisePostProcessor, Noise>> noisePostProcessorMap;

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
            noisePostProcessorMap = new LinkedList<>();
        }

        INSTANCE = this;
    }

    public Collection<ClientChunk> getAllClientChunks() {
        return clientChunkMap.values();
    }

    public void applyGenSettings(WorldGenNoiseSettings noiseSettings, HashMap<BiomeType, float[]> biomeDataMap, int worldSeed) {
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
            for(NoisePostProcessor npp : noiseSettings.postProcessList) {
                Noise noise = new Noise(worldSeed);
                npp.noiseWrapper.applyTo(noise);
                noisePostProcessorMap.add(new Pair<>(npp, noise));
                ExpoLogger.log(npp.noiseWrapper.name + ": " + noise.getSeed());
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
        ExpoClientContainer.get().getClientWorld().updateChunksToDraw();
    }

    public void updateChunkData(P11_ChunkData p) {
        String key = p.chunkX + "," + p.chunkY;
        ClientChunk existing = clientChunkMap.get(key);

        if(existing == null) {
            clientChunkMap.put(key, new ClientChunk(p.chunkX, p.chunkY, p.biomes, p.individualTileData, p.grassColor, p.tileEntityCount));
        } else {
            existing.update(p.biomes, p.individualTileData, p.grassColor, p.tileEntityCount);
        }
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(int x, int y) {
        return getBiomeData(x, y).key;
    }

    public float getElevation(int x, int y) {
        return getBiomeData(x, y).value;
    }

    public Pair<BiomeType, Float> getBiomeData(int x, int y) {
        String key = x + "," + y;
        Pair<BiomeType, Float> pair = noiseCacheMap.get(key);

        if(pair == null) {
            pair = convertNoise(x, y);
            noiseCacheMap.put(key, pair);
        }

        return pair;
    }

    private float normalized(Noise noise, int x, int y) {
        return (noise.getConfiguredNoise(x, y) + 1) * 0.5f;
    }

    private Pair<BiomeType, Float> convertNoise(int x, int y) {
        if(terrainNoiseHeight == null) return new Pair<>(BiomeType.VOID, 0f);

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
                if(toCheck != BiomeType.OCEAN_DEEP) {
                    float river = normalized(riverNoise, x, y);
                    if(river >= 0.975f) return new Pair<>(BiomeType.RIVER, river);
                }

                for(var pair : noisePostProcessorMap) {
                    if(pair.key.postProcessorLogic instanceof PostProcessorBiome ppb) {
                        float _norm = normalized(pair.value, x, y);
                        BiomeType biome = ppb.getBiome(toCheck, _norm);

                        if(biome != null) {
                            return new Pair<>(biome, _norm);
                        }
                    }
                }

                return new Pair<>(toCheck, height);
            }
        }

        return new Pair<>(BiomeType.VOID, 0f);
    }

    public ClientChunk getChunk(int x, int y) {
        return clientChunkMap.get(x + "," + y);
    }

    public static ClientChunkGrid get() {
        return INSTANCE;
    }

}