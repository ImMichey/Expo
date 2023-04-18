package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.gen.WorldGen;
import dev.michey.expo.server.main.logic.world.gen.WorldGenNoiseSettings;
import dev.michey.expo.server.main.logic.world.gen.WorldGenSettings;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;
import make.some.noise.Noise;

import java.io.File;
import java.util.*;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.PLAYER_CHUNK_VIEW_RANGE;
import static dev.michey.expo.util.ExpoShared.SPAWN_AREA_CHUNK_RANGE;

public class ServerChunkGrid {

    /** Which dimension does this chunk grid belong to? */
    private final ServerDimension dimension;

    /** Chunk file list */
    private final List<String> knownChunkFiles;

    /** Chunk logic */
    private final HashMap<String, Pair<ServerChunk, Long>> activeChunkMap; // key = hash, value = pair <chunk, activeTimestamp>
    private final HashMap<String, Pair<ServerChunk, Long>> inactiveChunkMap; // key = hash, value = pair <chunk, inactiveTimestamp>
    private long unloadAfterMillis;
    private long saveAfterMillis;
    private Pair[] spawnChunks; // can be null if not main dimension
    private final HashMap<String, ServerTile> tileMap;

    /** Noise logic */
    private final Noise terrainNoiseHeight;
    private final Noise terrainNoiseTemperature;
    private final Noise terrainNoiseMoisture;
    private final Noise riverNoise;
    private final HashMap<String, BiomeType> noiseCacheMap;

    /** Biome logic */
    private final WorldGenSettings genSettings;

    public ServerChunkGrid(ServerDimension dimension) {
        this.dimension = dimension;
        activeChunkMap = new HashMap<>();
        inactiveChunkMap = new HashMap<>();
        knownChunkFiles = new LinkedList<>();
        tileMap = new HashMap<>();
        unloadAfterMillis = ExpoShared.UNLOAD_CHUNKS_AFTER_MILLIS;
        saveAfterMillis = ExpoShared.SAVE_CHUNKS_AFTER_MILLIS;

        genSettings = WorldGen.get().getSettings(dimension.getDimensionName());
        WorldGenNoiseSettings settings = genSettings.getNoiseSettings();

        terrainNoiseHeight = new Noise(0);
        terrainNoiseTemperature = new Noise(0);
        terrainNoiseMoisture = new Noise(0);
        riverNoise = new Noise(0);

        if(settings != null) {
            if(settings.isTerrainGenerator()) {
                terrainNoiseHeight.setFrequency(settings.terrainElevationFrequency);
                terrainNoiseHeight.setNoiseType(settings.terrainElevationType);
                terrainNoiseHeight.setFractalOctaves(settings.terrainElevationOctaves);
                if(settings.terrainElevationFractalType != -1) terrainNoiseHeight.setFractalType(settings.terrainElevationFractalType);

                terrainNoiseTemperature.setFrequency(settings.terrainTemperatureFrequency);
                terrainNoiseTemperature.setNoiseType(settings.terrainTemperatureType);
                terrainNoiseTemperature.setFractalOctaves(settings.terrainTemperatureOctaves);
                if(settings.terrainTemperatureFractalType != -1) terrainNoiseTemperature.setFractalType(settings.terrainTemperatureFractalType);

                terrainNoiseMoisture.setFrequency(settings.terrainMoistureFrequency);
                terrainNoiseMoisture.setNoiseType(settings.terrainMoistureType);
                terrainNoiseMoisture.setFractalOctaves(settings.terrainMoistureOctaves);
                if(settings.terrainMoistureFractalType != -1) terrainNoiseMoisture.setFractalType(settings.terrainMoistureFractalType);
            }

            if(settings.isRiversGenerator()) {
                riverNoise.setFrequency(settings.noiseRiversFrequency);
                riverNoise.setNoiseType(settings.noiseRiversType);
                riverNoise.setFractalOctaves(settings.noiseRiversOctaves);
                if(settings.noiseRiversFractalType != -1) riverNoise.setFractalType(settings.noiseRiversFractalType);
            }
        }

        noiseCacheMap = new HashMap<>();
    }

    public void setUnloadAfterMillis(long unloadAfterMillis) {
        this.unloadAfterMillis = unloadAfterMillis;
    }

    public void setSaveAfterMillis(long saveAfterMillis) {
        this.saveAfterMillis = saveAfterMillis;
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(int x, int y) {
        String key = x + "," + y;
        return getBiome(x, y, key);
    }

    public BiomeType getBiome(int x, int y, String key) {
        BiomeType type = noiseCacheMap.get(key);

        if(type == null) {
            float normalizedHeight = genSettings.getNoiseSettings().isTerrainGenerator() ? ((terrainNoiseHeight.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedTemperature = genSettings.getNoiseSettings().isTerrainGenerator() ? ((terrainNoiseTemperature.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedMoisture = genSettings.getNoiseSettings().isTerrainGenerator() ? ((terrainNoiseMoisture.getConfiguredNoise(x, y) + 1) / 2f) : -1;
            float normalizedRiver = genSettings.getNoiseSettings().isRiversGenerator() ? ((riverNoise.getConfiguredNoise(x, y) + 1) / 2f) : -1;

            type = convertNoise(normalizedHeight, normalizedTemperature, normalizedMoisture, normalizedRiver);

            noiseCacheMap.put(key, type);
        }

        return type;
    }

    public ServerTile getTile(int x, int y) {
        String key = x + "," + y;
        return tileMap.get(key);
    }

    public void removeTile(int x, int y) {
        String key = x + "," + y;
        tileMap.remove(key);
    }

    public void addTile(ServerTile tile) {
        String key = tile.tileX + "," + tile.tileY;
        tileMap.put(key, tile);
    }

    private BiomeType convertNoise(float height, float temperature, float moisture, float river) {
        if(height == -1) return BiomeType.VOID;
        // riverNoise is ignored for now.

        for(BiomeType toCheck : genSettings.getBiomeDataMap().keySet()) {
            float[] values = genSettings.getBiomeDataMap().get(toCheck);

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

    /** Initializes the known chunk list. */
    public void initializeKnownChunks(File[] fileList) {
        log("Initializing known chunks for " + dimension.getDimensionName());

        if(fileList != null) {
            for(File file : fileList) {
                String name = file.getName();

                // Attempt to parse
                if(name.contains(",")) {
                    String[] split = name.split(",");

                    if(split.length == 2) {
                        var a = ExpoShared.asInt(split[0]);
                        var b = ExpoShared.asInt(split[1]);

                        if(a.value && b.value) {
                            knownChunkFiles.add(chunkHash(a.key, b.key));
                        }
                    }
                }
            }
        }

        log("    cached " + knownChunkFiles.size() + " chunk(s).");
    }

    /** Initializes the spawn chunks so they are always active. */
    public void initializeSpawnChunks(int startChunkX, int startChunkY) {
        spawnChunks = new Pair[SPAWN_AREA_CHUNK_RANGE * SPAWN_AREA_CHUNK_RANGE];

        for(int i = 0; i < spawnChunks.length; i++) {
            int x = i % SPAWN_AREA_CHUNK_RANGE;
            int y = i / SPAWN_AREA_CHUNK_RANGE;
            spawnChunks[i] = new Pair<>(startChunkX + x, startChunkY + y);
        }

        log("Loaded " + spawnChunks.length + " spawn chunks for dimension " + dimension.getDimensionName());
    }

    /** Main chunk logic method. */
    public void tickChunks() {
        long now = System.currentTimeMillis();

        // Update spawn chunks constantly, so they aren't marked as inactive
        if(spawnChunks != null) {
            for(Pair hash : spawnChunks) {
                ServerChunk chunk = getChunk((Integer) hash.key, (Integer) hash.value);
                activeChunkMap.get(chunk.getChunkKey()).value = generateInactiveChunkTimestamp();
            }
        }

        // Update chunks in proximity of players
        var entityManager = dimension.getEntityManager();
        LinkedList<ServerEntity> players = entityManager.getEntitiesOf(ServerEntityType.PLAYER);

        for(ServerEntity entity : players) {
            ServerPlayer player = (ServerPlayer) entity;
            ServerChunk[] chunksInView = getChunksInPlayerRange(player);

            for(ServerChunk chunk : chunksInView) {
                activeChunkMap.get(chunk.getChunkKey()).value = now;
            }
        }

        // Mark active chunks as inactive
        {
            Iterator<String> iterator = activeChunkMap.keySet().iterator();

            while(iterator.hasNext()) {
                String key = iterator.next();
                var pair = activeChunkMap.get(key);

                if(now - pair.value > 0) { // reached inactive state
                    iterator.remove();
                    pair.value = generateSaveChunkTimestamp();
                    inactiveChunkMap.put(key, pair);
                    pair.key.onInactive();
                }
            }
        }

        // Save inactive chunks
        {
            Iterator<String> iterator = inactiveChunkMap.keySet().iterator();

            while(iterator.hasNext()) {
                String key = iterator.next();
                var pair = inactiveChunkMap.get(key);

                if(now - pair.value > 0) { // reached inactive state
                    iterator.remove();
                    pair.key.onSave();
                }
            }
        }
    }

    /** Returns an array of all chunks that are in range of given player. */
    public ServerChunk[] getChunksInPlayerRange(ServerPlayer player) {
        int playerChunkX = ExpoShared.posToChunk(player.posX) - (PLAYER_CHUNK_VIEW_RANGE - 1) / 2;
        int playerChunkY = ExpoShared.posToChunk(player.posY) - (PLAYER_CHUNK_VIEW_RANGE - 1) / 2;

        ServerChunk[] chunks = new ServerChunk[PLAYER_CHUNK_VIEW_RANGE * PLAYER_CHUNK_VIEW_RANGE];

        for(int i = 0; i < chunks.length; i++) {
            int x = i % PLAYER_CHUNK_VIEW_RANGE;
            int y = i / PLAYER_CHUNK_VIEW_RANGE;
            chunks[i] = getChunk(playerChunkX + x, playerChunkY + y);
        }

        return chunks;
    }

    /** Returns an array of all chunk numbers that are in range of given player. */
    public int[] getChunkNumbersInPlayerRange(ServerPlayer player) {
        int playerChunkX = ExpoShared.posToChunk(player.posX) - (PLAYER_CHUNK_VIEW_RANGE - 1) / 2;
        int playerChunkY = ExpoShared.posToChunk(player.posY) - (PLAYER_CHUNK_VIEW_RANGE - 1) / 2;

        int[] chunks = new int[PLAYER_CHUNK_VIEW_RANGE * PLAYER_CHUNK_VIEW_RANGE * 2];
        int arrayPos = 0;

        for(int i = 0; i < chunks.length / 2; i++) {
            int x = i % PLAYER_CHUNK_VIEW_RANGE;
            int y = i / PLAYER_CHUNK_VIEW_RANGE;
            chunks[arrayPos    ] = playerChunkX + x;
            chunks[arrayPos + 1] = playerChunkY + y;
            arrayPos += 2;
        }

        return chunks;
    }

    public boolean isActiveChunk(int chunkX, int chunkY) {
        return activeChunkMap.containsKey(chunkHash(chunkX, chunkY));
    }

    /** Returns the chunk at chunk coordinates X & Y. */
    public ServerChunk getChunk(int chunkX, int chunkY) {
        // Generate chunk hash.
        String hash = chunkHash(chunkX, chunkY);

        // Return active chunk if it already exists.
        var active = activeChunkMap.get(hash);
        if(active != null) return active.key;

        // Return inactive chunk and mark it as active if it already exists.
        var inactive = inactiveChunkMap.get(hash);
        if(inactive != null) {
            inactiveChunkMap.remove(hash);
            activeChunkMap.put(hash, new Pair<>(inactive.key, generateInactiveChunkTimestamp()));
            inactive.key.onActive();
            return inactive.key;
        }

        // Generate/load chunk.
        ServerChunk chunk = new ServerChunk(dimension, chunkX, chunkY);

        if(knownChunkFiles.contains(hash)) {
            // Chunk is stored as a file, load and cache.
            chunk.loadFromFile();
        } else {
            // Generate chunk.
            chunk.generate(true);
        }

        activeChunkMap.put(hash, new Pair<>(chunk, generateInactiveChunkTimestamp()));
        return chunk;
    }

    /** Returns the hashed value of a chunk coordinate. */
    private String chunkHash(int x, int y) {
        return x + "," + y;
    }

    /** Returns the timestamp when an active chunk should be marked as inactive if there are no updates during that time. */
    private long generateInactiveChunkTimestamp() {
        return System.currentTimeMillis() + unloadAfterMillis;
    }

    /** Returns the timestamp when an inactive chunk should be saved to the disk. */
    private long generateSaveChunkTimestamp() {
        return System.currentTimeMillis() + saveAfterMillis;
    }

    /** Called when the server shuts down and all chunks in memory have to be saved. */
    public void saveAllChunks() {
        log("Saving all chunks for dimension " + dimension.getDimensionName());
        for(var v : activeChunkMap.values()) {
            v.key.onInactive();
            v.key.onSave();
        }

        for(var v : inactiveChunkMap.values()) {
            v.key.onSave();
        }
    }

    public void chunkdump() {
        log("Inactive/Active chunks: " + inactiveChunkMap.size() + "/" + activeChunkMap.size());

        log("=== Active chunks ===");
        for(String active : activeChunkMap.keySet()) {
            long ts = activeChunkMap.get(active).value;
            log(active + " ts: " + ts + " (" + (ts - System.currentTimeMillis()) + ")");
        }

        log("=== Inactive chunks ===");
        for(String inactive : inactiveChunkMap.keySet()) {
            long ts = inactiveChunkMap.get(inactive).value;
            log(inactive + " ts: " + ts + " (" + (ts - System.currentTimeMillis()) + ")");
        }
    }

    public Noise getTerrainNoiseHeight() {
        return terrainNoiseHeight;
    }

    public Noise getTerrainNoiseMoisture() {
        return terrainNoiseMoisture;
    }

    public Noise getTerrainNoiseTemperature() {
        return terrainNoiseTemperature;
    }

    public Noise getRiverNoise() {
        return riverNoise;
    }

    public WorldGenSettings getGenSettings() {
        return genSettings;
    }

}
