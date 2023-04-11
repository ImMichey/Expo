package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.math.Interpolation;
import com.esotericsoftware.kryonet.Server;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.main.logic.world.gen.WorldGen;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;
import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;
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
    public WorldGenSettings genSettings;
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
            noise = new Noise();
            riverNoise = new Noise();
            noiseCacheMap = new HashMap<>();
        }

        INSTANCE = this;
    }

    public void applyGenSettings(WorldGenSettings genSettings) {
        this.genSettings = genSettings;
        log("Applying world gen mapping " + genSettings);

        WorldGenNoiseSettings s = genSettings.getNoiseSettings();

        if(s.isTerrainGenerator()) {
            noise.setFrequency(s.noiseTerrainFrequency);
            noise.setNoiseType(s.noiseTerrainType);
            noise.setFractalOctaves(s.noiseTerrainOctaves);
            if(s.noiseTerrainFractalType != -1) noise.setFractalType(s.noiseTerrainFractalType);
        }

        if(s.isRiversGenerator()) {
            riverNoise.setFrequency(s.noiseRiversFrequency);
            riverNoise.setNoiseType(s.noiseRiversType);
            riverNoise.setFractalOctaves(s.noiseRiversOctaves);
            if(s.noiseTerrainFractalType != -1) riverNoise.setFractalType(s.noiseRiversFractalType);
        }
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

    public void updateChunkData(int chunkX, int chunkY, ServerTile[] tiles) {
        String key = chunkX + "," + chunkY;
        clientChunkMap.put(key, new ClientChunk(chunkX, chunkY, tiles));
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(int x, int y) {
        String key = x + "," + y;
        return getBiome(x, y, key);
    }

    public BiomeType getBiome(int x, int y, String key) {
        BiomeType type = noiseCacheMap.get(key);

        if(type == null) {
            float normalizedNoise = genSettings.getNoiseSettings().isTerrainGenerator() ? ((noise.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedRiver = genSettings.getNoiseSettings().isRiversGenerator() ? ((riverNoise.getConfiguredNoise(x, y) + 1) / 2f) : -1;

            type = convertNoise(normalizedNoise, normalizedRiver);
            noiseCacheMap.put(key, type);
        }

        return type;
    }

    private BiomeType convertNoise(float noise, float riverNoise) {
        BiomeType fit = BiomeType.VOID;
        float nearestElevation = 1.0f;
        if(noise == -1) return fit;
        // riverNoise is ignored for now.

        for(BiomeType toCheck : genSettings.getBiomeDataMap().keySet()) {
            float[] values = genSettings.getBiomeDataMap().get(toCheck);
            float elevation = values[0];

            if(noise <= elevation && elevation <= nearestElevation) {
                fit = toCheck;
                nearestElevation = values[0];
            }

            // temperature, moisture is ignored for now.
        }

        return fit;
    }

    public ClientChunk getChunk(int x, int y) {
        return clientChunkMap.get(x + "," + y);
    }

    public static ClientChunkGrid get() {
        return INSTANCE;
    }

}
