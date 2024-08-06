package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.Expo;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.gen.BiomeDefinition;
import dev.michey.expo.server.main.logic.world.gen.NoisePostProcessor;
import dev.michey.expo.server.main.logic.world.gen.PostProcessorBiome;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;
import dev.michey.expo.server.packet.P11_ChunkData;
import dev.michey.expo.util.Pair;
import make.some.noise.Noise;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class ClientChunkGrid {

    private static ClientChunkGrid INSTANCE;

    private final ConcurrentHashMap<String, ClientChunk> clientChunkMap;
    public final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public final ConcurrentLinkedQueue<P11_ChunkData> queuedChunkDataList;

    /** Minimap only noise logic on dedicated server connection. */
    public Noise terrainNoiseHeight;
    public Noise terrainNoiseTemperature;
    public Noise terrainNoiseMoisture;
    public WorldGenNoiseSettings noiseSettings;
    public List<BiomeDefinition> biomeDefinitionList;

    private ConcurrentSkipListMap<String, Pair<BiomeType, float[]>> noiseCacheMap;
    private HashMap<String, Noise> noisePostProcessorMap;

    /** Water wave logic */
    private float waveDelta;
    private float waveCooldown;
    private float factor = 1.0f;
    public float interpolation;

    public ClientChunkGrid() {
        clientChunkMap = new ConcurrentHashMap<>();
        queuedChunkDataList = new ConcurrentLinkedQueue<>();

        if(Expo.get().isMultiplayer()) {
            // Create a client-sided noise generator as we cannot access the server-sided noise/chunk data
            terrainNoiseHeight = new Noise();
            terrainNoiseTemperature = new Noise();
            terrainNoiseMoisture = new Noise();
            noiseCacheMap = new ConcurrentSkipListMap<>();
            noisePostProcessorMap = new HashMap<>();
        }

        INSTANCE = this;
    }

    public Collection<ClientChunk> getAllChunks() {
        return clientChunkMap.values();
    }

    public void applyGenSettings(WorldGenNoiseSettings noiseSettings, List<BiomeDefinition> biomeDefinitionList, int worldSeed) {
        this.noiseSettings = noiseSettings;
        this.biomeDefinitionList = biomeDefinitionList;
        //log("Applying world gen mapping " + noiseSettings);

        if(noiseSettings.isTerrainGenerator()) {
            noiseSettings.terrainElevation.applyTo(terrainNoiseHeight);
            noiseSettings.terrainTemperature.applyTo(terrainNoiseTemperature);
            noiseSettings.terrainMoisture.applyTo(terrainNoiseMoisture);
        }

        if(noiseSettings.isPostProcessorGenerator()) {
            for(NoisePostProcessor wrapper : noiseSettings.postProcessList) {
                Noise noise = new Noise(worldSeed);
                wrapper.noiseWrapper.applyTo(noise);
                noisePostProcessorMap.put(wrapper.noiseWrapper.name, noise);
            }

            noiseSettings.postProcessList.sort(Comparator.comparingInt(o -> -o.priority));
        }
    }

    public void handleChunkData(P11_ChunkData p) {
        queuedChunkDataList.add(p);
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

        while(!queuedChunkDataList.isEmpty()) {
            P11_ChunkData polledChunk = queuedChunkDataList.poll();
            executorService.execute(() -> updateChunkData(polledChunk));
        }

        interpolation = Interpolation.smooth2.apply(waveDelta) * WAVE_STRENGTH;
        ExpoClientContainer.get().getClientWorld().updateChunksToDraw();
    }

    public void updateChunkData(P11_ChunkData p) {
        String key = p.chunkX + "," + p.chunkY;
        ClientChunk existing = clientChunkMap.get(key);

        if(existing == null) {
            ClientChunk cc = new ClientChunk(p.chunkX, p.chunkY, p.biomes, p.individualTileData, p.tileEntityCount);
            clientChunkMap.put(key, cc);
        } else {
            existing.update(p.biomes, p.individualTileData, p.tileEntityCount);
        }
    }

    public ClientChunk getChunk(int x, int y) {
        return clientChunkMap.get(x + "," + y);
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(String dimension, int x, int y) {
        return getBiomeData(dimension, x, y).key;
    }

    public float[] getElevationTemperatureMoisture(String dimension, int x, int y) {
        return getBiomeData(dimension, x, y).value;
    }

    public Pair<BiomeType, float[]> getBiomeData(String dimension, int x, int y) {
        if(Expo.get().isMultiplayer()) {
            // Generate client-sided
            String key = x + "," + y;
            Pair<BiomeType, float[]> pair = noiseCacheMap.get(key);

            if(pair == null) {
                pair = convertNoise(dimension, x, y);
                noiseCacheMap.put(key, pair);
            }

            return pair;
        } else {
            return ServerWorld.get().getDimension(dimension).getChunkHandler().getBiomeData(x, y);
        }
    }

    private float normalized(Noise noise, int x, int y) {
        return (noise.getConfiguredNoise(x, y) + 1) * 0.5f;
    }

    private Pair<BiomeType, float[]> convertNoise(String dimension, int x, int y) {
        if(noiseSettings.isTerrainGenerator()) return new Pair<>(BiomeType.VOID, new float[] {0, 0, 0});

        for(BiomeDefinition biomeDefinition : biomeDefinitionList) {
            float[] values = biomeDefinition.noiseBoundaries;
            BiomeType toCheck = biomeDefinition.biomeType;
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
                for(NoisePostProcessor npp : noiseSettings.postProcessList) {
                    if(npp.postProcessorLogic instanceof PostProcessorBiome ppb) {
                        float _norm = normalized(noisePostProcessorMap.get(ppb.noiseName), x, y);
                        BiomeType biome = ppb.getBiome(toCheck, _norm);

                        if(biome != null) {
                            return new Pair<>(biome, new float[] {_norm, temperature, moisture});
                        }
                    }
                }

                return new Pair<>(toCheck, new float[] {height, temperature, moisture});
            }
        }

        return new Pair<>(BiomeType.VOID, new float[] {0, 0, 0});
    }

    public static ClientChunkGrid get() {
        return INSTANCE;
    }

}