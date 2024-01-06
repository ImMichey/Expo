package dev.michey.expo.server.main.logic.world.spawn;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.*;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class EntitySpawnManager {

    private final ServerDimension dimension;
    private final LinkedList<EntitySpawner> spawnerList;
    private final HashMap<EntitySpawner, Float> nextSpawnMap;

    private final HashMap<ServerChunk, Integer> entitiesPerChunkMap;
    private final HashMap<ServerChunk, List<ServerPlayer>> playersInChunkMap;
    private final HashMap<ServerChunk, Pair<int[], Integer>> applicableSpawnTileMap;
    private final HashMap<ServerPlayer, Integer> playerEntityTrackerMap;
    private final LinkedList<ServerChunk> populationChunkCandidateList;

    public EntitySpawnManager(ServerDimension dimension, LinkedList<EntitySpawner> spawnerList) {
        this.dimension = dimension;
        this.spawnerList = spawnerList;
        nextSpawnMap = new HashMap<>();
        entitiesPerChunkMap = new HashMap<>();
        playersInChunkMap = new HashMap<>();
        playerEntityTrackerMap = new HashMap<>();
        applicableSpawnTileMap = new HashMap<>();
        populationChunkCandidateList = new LinkedList<>();

        for(EntitySpawner es : spawnerList) {
            nextSpawnMap.put(es, es.spawnTimer);
        }
    }

    public void tick(float delta) {
        for(EntitySpawner es : spawnerList) {
            float post = nextSpawnMap.get(es) - delta;

            if(post <= 0) {
                // Run algorithm
                nextSpawnMap.put(es, post + es.spawnTimer);
                entitiesPerChunkMap.clear();
                playersInChunkMap.clear();
                playerEntityTrackerMap.clear();
                applicableSpawnTileMap.clear();
                populationChunkCandidateList.clear();

                // ========================================= TIME FRAME CHECK
                int[] timeframes = es.spawnTimeframes;
                float currentTime = dimension.dimensionTime;
                boolean inTimeFrame = false;

                for(int i = 0; i < timeframes.length; i += 2) {
                    int begin = timeframes[i  ];
                    int end = timeframes[i + 1];

                    if(currentTime >= begin && currentTime <= end) {
                        inTimeFrame = true;
                        break;
                    }
                }

                if(!inTimeFrame) continue;
                captureEntitySnapshot(es);

                nextChunk: for(Map.Entry<ServerChunk, Integer> entrySet : entitiesPerChunkMap.entrySet()) {
                    int existing = entitiesPerChunkMap.get(entrySet.getKey());

                    if(es.entityCapPerPlayer == -1) {
                        populationChunkCandidateList.add(entrySet.getKey());
                    } else {
                        if(existing < es.entityCapPerPlayer) {
                            for(ServerPlayer inRange : playersInChunkMap.get(entrySet.getKey())) {
                                int playerIndividualCap = playerEntityTrackerMap.get(inRange);

                                if(playerIndividualCap >= es.entityCapPerPlayer) {
                                    continue nextChunk;
                                }
                            }

                            populationChunkCandidateList.add(entrySet.getKey());
                        }
                    }
                }

                // Shuffle so spawns are more randomized each run.
                // Each chunk has < entityCapPerPlayer entities.
                Collections.shuffle(populationChunkCandidateList, ExpoShared.RANDOM);

                nextPlayer: for(Map.Entry<ServerPlayer, Integer> entrySet : playerEntityTrackerMap.entrySet()) {
                    int existingFor = playerEntityTrackerMap.get(entrySet.getKey());

                    if(es.entityCapPerPlayer != -1 && existingFor >= es.entityCapPerPlayer) {
                        continue;
                    }

                    nextChunk: for(ServerChunk candidateChunk : populationChunkCandidateList) {
                        int playersInRange = playersInChunkMap.get(candidateChunk).size();
                        boolean attemptSpawnInThisChunk = (es.spawnChance * (ROW_TILES * ROW_TILES) * playersInRange) >= MathUtils.random();
                        if(!attemptSpawnInThisChunk) continue;

                        int toSpawn = MathUtils.random(es.spawnMin, es.spawnMax);
                        Pair<int[], Integer> pair = applicableSpawnTileMap.get(candidateChunk);

                        for(int i = 0; i < pair.value; i++) {
                            float chance = MathUtils.random();

                            if(chance <= es.spawnChance) {
                                ServerTile tile = candidateChunk.tiles[pair.key[i]];
                                float worldX = ExpoShared.tileToPos(tile.tileX) + 8f;
                                float worldY = ExpoShared.tileToPos(tile.tileY) + 8f;

                                if(isFarEnoughFromPlayers(worldX, worldY, es.playerDistanceMin)) {
                                    toSpawn--;

                                    ServerEntity spawned = ServerEntityType.typeToEntity(es.spawnType);
                                    spawned.posX = worldX;
                                    spawned.posY = worldY;
                                    spawned.onGeneration(false, tile.biome, new GenerationRandom(tile.chunk.chunkX, tile.chunk.chunkY, es.spawnType));
                                    ServerWorld.get().registerServerEntity(dimension.getDimensionName(), spawned);

                                    int newValueChunk = entitiesPerChunkMap.get(candidateChunk) + 1;
                                    int newValuePlayer = playerEntityTrackerMap.get(entrySet.getKey()) + 1;
                                    entitiesPerChunkMap.put(candidateChunk, newValueChunk);
                                    playerEntityTrackerMap.put(entrySet.getKey(), newValuePlayer);

                                    if(newValuePlayer >= es.entityCapPerPlayer) {
                                        continue nextPlayer;
                                    }

                                    if(toSpawn <= 0) {
                                        continue nextChunk;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                nextSpawnMap.put(es, post);
            }
        }
    }

    private boolean isApplicableBiome(EntitySpawner es, BiomeType type) {
        for(BiomeType t : es.spawnBiomes) {
            if(t == type) return true;
        }

        return false;
    }

    private boolean isFarEnoughFromPlayers(float x, float y, float minDis) {
        if(minDis == -1) return true;
        var players = dimension.getEntityManager().getAllPlayers();

        for(ServerPlayer player : players) {
            if(Vector2.dst(player.posX, player.posY, x, y) < minDis) {
                return false;
            }
        }

        return true;
    }

    private void captureEntitySnapshot(EntitySpawner es) {
        LinkedList<ServerEntity> existing = dimension.getEntityManager().getEntitiesOf(es.spawnType);
        Collection<Pair<ServerChunk, Long>> chunks = dimension.getChunkHandler().getActiveChunks();

        for(Pair<ServerChunk, Long> chunkKey : chunks) {
            ServerChunk chunk = chunkKey.key;
            int cx = chunk.chunkX;
            int cy = chunk.chunkY;
            int entityCount = 0;

            for(ServerEntity seekingEntity : existing) {
                if(seekingEntity.chunkX == cx && seekingEntity.chunkY == cy) {
                    entityCount++;
                }
            }

            entitiesPerChunkMap.put(chunk, entityCount);
        }

        if(es.entityCapPerPlayer != -1) {
            LinkedList<ServerPlayer> players = dimension.getEntityManager().getAllPlayers();

            for(Pair<ServerChunk, Long> chunkKey : chunks) {
                ServerChunk chunk = chunkKey.key;
                int cx = chunk.chunkX;
                int cy = chunk.chunkY;

                int[] tiles = new int[ROW_TILES * ROW_TILES];
                int totalApplicable = 0;

                for(ServerTile tile : chunk.tiles) {
                    if(isApplicableBiome(es, tile.biome)) {
                        tiles[totalApplicable] = tile.tileArray;
                        totalApplicable++;
                    }
                }

                applicableSpawnTileMap.put(chunk, new Pair<>(tiles, totalApplicable));

                LinkedList<ServerPlayer> playerList = new LinkedList<>();

                for(ServerPlayer player : players) {
                    int perPlayerCount = 0;
                    int viewportStartX = player.chunkX - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                    int viewportStartY = player.chunkY - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;
                    int viewportEndX = player.chunkX + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                    int viewportEndY = player.chunkY + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;

                    if(cx >= viewportStartX && cx <= viewportEndX && cy >= viewportStartY && cy <= viewportEndY) {
                        playerList.add(player);
                    }

                    for(ServerEntity existingEntity : existing) {
                        int eex = existingEntity.chunkX;
                        int eey = existingEntity.chunkY;

                        if(eex >= viewportStartX && eex <= viewportEndX && eey >= viewportStartY && eey <= viewportEndY) {
                            perPlayerCount++;
                        }
                    }

                    playerEntityTrackerMap.put(player, perPlayerCount);
                }

                playersInChunkMap.put(chunk, playerList);
            }
        }
    }

}