package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.gen.*;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;
import make.some.noise.Noise;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.*;

public class ServerChunkGrid {

    /** Which dimension does this chunk grid belong to? */
    private final ServerDimension dimension;

    /** Chunk file list */
    private final List<String> knownChunkFiles;

    /** Chunk logic */
    private long unloadAfterMillis;
    private long saveAfterMillis;
    private long lastSaveCheckMillis;

    /** Multithreading logic */
    public final ConcurrentHashMap<String, Pair<ServerChunk, Long>> activeChunkMap; // key = hash, value = pair <chunk, activeTimestamp>
    public final ConcurrentHashMap<String, Pair<ServerChunk, Long>> inactiveChunkMap; // key = hash, value = pair <chunk, inactiveTimestamp>
    private final ConcurrentHashMap<String, ServerTile> tileMap;
    public final ConcurrentHashMap<String, Set<Integer>> generatingChunkMap; // key = hash, value = pair <chunk, list<playerIds>>
    private final HashMap<ServerPlayer, PlayerChunkMemory> playerChunkMemoryMap;

    /** Noise logic */
    private final Noise terrainNoiseHeight;
    private final Noise terrainNoiseTemperature;
    private final Noise terrainNoiseMoisture;
    private final HashMap<String, Noise> noisePostProcessorMap;
    private final ConcurrentSkipListMap<String, Pair<BiomeType, float[]>> noiseCacheMap;
    //private final ConcurrentHashMap<String, Pair<BiomeType, float[]>> noiseCacheMap;

    /** Biome logic */
    private final WorldGenSettings genSettings;

    public ServerChunkGrid(ServerDimension dimension) {
        this.dimension = dimension;
        activeChunkMap = new ConcurrentHashMap<>();
        inactiveChunkMap = new ConcurrentHashMap<>();
        knownChunkFiles = new LinkedList<>();
        tileMap = new ConcurrentHashMap<>();
        noisePostProcessorMap = new HashMap<>();
        playerChunkMemoryMap = new HashMap<>();
        generatingChunkMap = new ConcurrentHashMap<>();
        noiseCacheMap = new ConcurrentSkipListMap<>();
        unloadAfterMillis = ExpoShared.UNLOAD_CHUNKS_AFTER_MILLIS;
        saveAfterMillis = ExpoShared.SAVE_CHUNKS_AFTER_MILLIS;

        genSettings = WorldGen.get().getSettings(dimension.getDimensionName());
        WorldGenNoiseSettings settings = genSettings.getNoiseSettings();

        terrainNoiseHeight = new Noise(0);
        terrainNoiseTemperature = new Noise(0);
        terrainNoiseMoisture = new Noise(0);

        if(settings != null) {
            if(settings.isTerrainGenerator()) {
                settings.terrainElevation.applyTo(terrainNoiseHeight);
                settings.terrainTemperature.applyTo(terrainNoiseTemperature);
                settings.terrainMoisture.applyTo(terrainNoiseMoisture);
            }

            if(settings.isPostProcessorGenerator()) {
                for(NoisePostProcessor wrapper : settings.postProcessList) {
                    Noise noise = new Noise(0);
                    wrapper.noiseWrapper.applyTo(noise);
                    noisePostProcessorMap.put(wrapper.noiseWrapper.name, noise);
                }
            }
        }
    }

    public void setUnloadAfterMillis(long unloadAfterMillis) {
        this.unloadAfterMillis = unloadAfterMillis;
    }

    public void setSaveAfterMillis(long saveAfterMillis) {
        this.saveAfterMillis = saveAfterMillis;
    }

    /** Returns the BiomeType at tile position X & Y. */
    public BiomeType getBiome(int x, int y) {
        return getBiomeData(x, y).key;
    }

    public float[] getElevationTemperatureMoisture(int x, int y) {
        return getBiomeData(x, y).value;
    }

    public Pair<BiomeType, float[]> getBiomeData(int x, int y) {
        String key = x + "," + y;
        Pair<BiomeType, float[]> pair = noiseCacheMap.get(key);

        if(pair == null) {
            pair = convertNoise(x, y);
            noiseCacheMap.put(key, pair);
        }

        return pair;
    }

    public float normalized(Noise noise, int x, int y) {
        return (noise.getConfiguredNoise(x, y) + 1) * 0.5f;
    }

    public void removeNoiseCache(int x, int y) {
        String key = x + "," + y;
        noiseCacheMap.remove(key);
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

    public TileLayerType getTileLayerType(int x, int y, int layer) {
        String k = x + "," + y;
        ServerTile existingTile = tileMap.get(k);

        if(existingTile != null && existingTile.dynamicTileParts != null) {
            return existingTile.dynamicTileParts[layer].emulatingType;
        }

        BiomeType b = getBiome(x, y);
        return TileLayerType.biomeToLayer(b, layer);
    }

    private Pair<BiomeType, float[]> convertNoise(int x, int y) {
        if(!genSettings.getNoiseSettings().isTerrainGenerator()) return new Pair<>(BiomeType.VOID, new float[] {0, 0, 0});

        for(BiomeDefinition biomeDefinition : genSettings.getBiomeDefinitionList()) {
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
                for(NoisePostProcessor npp : dimension.getChunkHandler().getGenSettings().getNoiseSettings().postProcessList) {
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

    /** Main chunk logic method. */
    public void tickChunks() {
        long now = System.currentTimeMillis();

        // Update chunks in proximity of players
        var entityManager = dimension.getEntityManager();
        List<ServerPlayer> players = entityManager.getAllPlayers();

        for(ServerPlayer player : players) {
            ServerChunk[] chunksInView = getChunksInPlayerRange(player);

            for(ServerChunk chunk : chunksInView) {
                if(chunk == null) continue;
                activeChunkMap.get(chunk.getChunkKey()).value = generateInactiveChunkTimestamp();
            }
        }

        // Mark active chunks as inactive
        {
            Iterator<Pair<ServerChunk, Long>> iterator = activeChunkMap.values().iterator();

            while(iterator.hasNext()) {
                Pair<ServerChunk, Long> pair = iterator.next();

                if((now - pair.value) > 0) { // reached inactive state
                    iterator.remove();
                    pair.value = generateTimestamp();
                    inactiveChunkMap.put(pair.key.getChunkKey(), pair);
                    pair.key.onInactive();
                }
            }
        }

        // Water spread logic
        for(Pair<ServerChunk, Long> pair : activeChunkMap.values()) {
            ServerChunk sc = pair.key;

            if(sc.ready) {
                if(sc.checkWaterSpread) {
                    sc.checkWaterSpread = false;
                    sc.doWaterSpreadCheck();
                } else {
                    sc.tickWaterSpread();
                }
            }
        }

        long diff = now - lastSaveCheckMillis;
        if(diff < 1000L) return;
        lastSaveCheckMillis = now;

        // Save inactive chunks
        {
            Iterator<Pair<ServerChunk, Long>> iterator = inactiveChunkMap.values().iterator();

            while(iterator.hasNext()) {
                Pair<ServerChunk, Long> pair = iterator.next();

                if(now - pair.value > 0) { // reached save state
                    iterator.remove();
                    knownChunkFiles.add(pair.key.getChunkKey());
                    pair.key.onSave(ServerWorld.get().mtVirtualService, false);
                }
            }
        }
    }

    /** Returns all active chunks with their respective inactivity timestamp. */
    public Collection<Pair<ServerChunk, Long>> getActiveChunks() {
        return activeChunkMap.values();
    }

    public void removeFromChunkMemoryMap(ServerPlayer player) {
        playerChunkMemoryMap.remove(player);
    }

    /** Returns an array of all chunks that are in range of given player. The existence of given chunks is not guaranteed. */
    public ServerChunk[] getChunksInPlayerRange(ServerPlayer player) {
        int playerChunkX = ExpoShared.posToChunk(player.posX) - PLAYER_CHUNK_VIEW_RANGE_DIR_X;
        int playerChunkY = ExpoShared.posToChunk(player.posY) - PLAYER_CHUNK_VIEW_RANGE_DIR_Y;
        var existing = playerChunkMemoryMap.get(player);
        boolean update = true;

        if(existing != null) {
            if(existing.originChunkX() == playerChunkX && existing.originChunkY() == playerChunkY) {
                if(existing.chunkArray() != null) {
                    ServerChunk[] sc = existing.chunkArray();
                    int valid = 0;

                    for(ServerChunk chunk : sc) {
                        if(chunk != null) {
                            valid++;
                        } else {
                            break;
                        }
                    }

                    if(valid == existing.chunkArray().length) {
                        update = false;
                    }
                }
            }
        }

        if(update) {
            ServerChunk[] chunks = new ServerChunk[PLAYER_CHUNK_VIEW_RANGE_X * PLAYER_CHUNK_VIEW_RANGE_Y];

            for(int i = 0; i < chunks.length; i++) {
                int x = i % PLAYER_CHUNK_VIEW_RANGE_X;
                int y = i / PLAYER_CHUNK_VIEW_RANGE_X;
                chunks[i] = getChunkUnsafe(player, playerChunkX + x, playerChunkY + y);
            }

            playerChunkMemoryMap.put(player, new PlayerChunkMemory(player, playerChunkX, playerChunkY, chunks));
            return chunks;
        }

        return existing.chunkArray();
    }

    /** Returns an array of all chunk numbers that are in range of given player. */
    public int[] getChunkNumbersInPlayerRange(ServerPlayer player) {
        int playerChunkX = ExpoShared.posToChunk(player.posX) - PLAYER_CHUNK_VIEW_RANGE_DIR_X;
        int playerChunkY = ExpoShared.posToChunk(player.posY) - PLAYER_CHUNK_VIEW_RANGE_DIR_Y;

        int[] chunks = new int[PLAYER_CHUNK_VIEW_RANGE_X * PLAYER_CHUNK_VIEW_RANGE_Y * 2];
        int arrayPos = 0;

        for(int i = 0; i < chunks.length / 2; i++) {
            int x = i % PLAYER_CHUNK_VIEW_RANGE_X;
            int y = i / PLAYER_CHUNK_VIEW_RANGE_X;
            chunks[arrayPos    ] = playerChunkX + x;
            chunks[arrayPos + 1] = playerChunkY + y;
            arrayPos += 2;
        }

        return chunks;
    }

    public boolean isActiveChunk(int chunkX, int chunkY) {
        return activeChunkMap.containsKey(chunkHash(chunkX, chunkY));
    }

    /** Returns the chunk at coordinates X & Y if present. If not present, it will generate the chunk in the background. */
    public ServerChunk getChunkUnsafe(ServerPlayer requester, int chunkX, int chunkY) {
        String hash = chunkHash(chunkX, chunkY);

        var active = activeChunkMap.get(hash);
        if(active != null) return active.key;

        var inactive = inactiveChunkMap.get(hash);

        if(inactive != null) {
            inactiveChunkMap.remove(hash);
            activeChunkMap.put(hash, new Pair<>(inactive.key, generateInactiveChunkTimestamp()));
            inactive.key.onActive();
            return inactive.key;
        }

        startChunkGeneration(requester, hash, chunkX, chunkY);
        return null;
    }

    private void startChunkGeneration(ServerPlayer requester, String hash, int chunkX, int chunkY) {
        AtomicBoolean resume = new AtomicBoolean(true);

        generatingChunkMap.computeIfPresent(hash, (s, serverChunkSetPair) -> {
            serverChunkSetPair.add(requester.entityId);
            resume.set(false);
            return serverChunkSetPair;
        });

        if(!resume.get()) {
            return;
        }

        HashSet<Integer> notifyList = new HashSet<>();
        notifyList.add(requester.entityId);
        generatingChunkMap.put(hash, notifyList);

        if(knownChunkFiles.contains(hash)) {
            // Start a I/O thread and load the chunk content.
            ServerWorld.get().mtServiceTick.execute(() -> {
                ServerChunk chunk = new ServerChunk(dimension, chunkX, chunkY);
                chunk.loadFromFile();
                chunk.ready = true;
                activeChunkMap.put(hash, new Pair<>(chunk, generateInactiveChunkTimestamp()));
                generatingChunkMap.remove(hash);
            });
        } else {
            // Start generation task.
            ServerWorld.get().mtServiceTick.execute(() -> {
                ServerChunk chunk = new ServerChunk(dimension, chunkX, chunkY);
                chunk.generate(true);
                chunk.ready = true;
                activeChunkMap.put(hash, new Pair<>(chunk, generateInactiveChunkTimestamp()));
                generatingChunkMap.remove(hash);
            });
        }
    }

    /** Returns the chunk at chunk coordinates X & Y. */
    public ServerChunk getChunkSafe(int chunkX, int chunkY) {
        return getChunkSafe(chunkX, chunkY, true);
    }

    public ServerChunk getChunkSafe(int chunkX, int chunkY, boolean populate) {
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
            chunk.generate(populate);
        }

        chunk.ready = true;
        activeChunkMap.put(hash, new Pair<>(chunk, generateInactiveChunkTimestamp()));
        return chunk;
    }

    public ServerChunk getActiveChunk(String chunkKey) {
        return activeChunkMap.get(chunkKey).key;
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
    public long generateTimestamp() {
        return System.currentTimeMillis() + saveAfterMillis;
    }

    /** Called when the server shuts down and all chunks in memory have to be saved. */
    public void saveAllChunks(ExecutorService shutdownService) {
        log("Saving all chunks for dimension " + dimension.getDimensionName() + " - Active/Inactive: " + activeChunkMap.size() + "/" + inactiveChunkMap.size());

        for(var v : activeChunkMap.values()) {
            v.key.onInactive();
            v.key.onSave(shutdownService, true);
        }

        for(var v : inactiveChunkMap.values()) {
            v.key.onSave(shutdownService, true);
        }
    }

    public void chunkdump() {
        log("Inactive/Active chunks: " + inactiveChunkMap.size() + "/" + activeChunkMap.size());

        log("=== Active chunks ===");
        for(var set : activeChunkMap.entrySet()) {
            long ts = set.getValue().value;
            log(set.getKey() + " ts: " + ts + " (" + (ts - System.currentTimeMillis()) + ")");
        }

        log("=== Inactive chunks ===");
        for(var set : inactiveChunkMap.entrySet()) {
            long ts = set.getValue().value;
            log(set.getKey() + " ts: " + ts + " (" + (ts - System.currentTimeMillis()) + ")");
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

    public HashMap<String, Noise> getNoisePostProcessorMap() {
        return noisePostProcessorMap;
    }

    public WorldGenSettings getGenSettings() {
        return genSettings;
    }

}
