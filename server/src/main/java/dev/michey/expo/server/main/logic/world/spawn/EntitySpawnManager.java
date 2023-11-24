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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class EntitySpawnManager {

    private final ServerDimension dimension;
    private final LinkedList<EntitySpawner> spawnerList;
    private final HashMap<EntitySpawner, Float> nextSpawnMap;

    public EntitySpawnManager(ServerDimension dimension, LinkedList<EntitySpawner> spawnerList) {
        this.dimension = dimension;
        this.spawnerList = spawnerList;
        nextSpawnMap = new HashMap<>();

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
                // ========================================= LIMIT CHECK
                HashMap<ServerPlayer, Integer> map = null;
                List<ServerPlayer> revisit = null;

                if(es.entityCapPerPlayer != -1) {
                    map = getExistingEntityPage(es);
                    revisit = new LinkedList<>();
                }

                // ========================================= BIOME CHECK
                var chunks = dimension.getChunkHandler().getActiveChunks();

                nextChunk: for(var pair : chunks) {
                    ServerChunk chunk = pair.key;

                    if(es.entityCapPerPlayer != -1) {
                        revisit.clear();

                        // Limitation check
                        for(ServerPlayer existingPlayer : map.keySet()) {
                            int viewportStartX = existingPlayer.chunkX - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                            int viewportStartY = existingPlayer.chunkY - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;
                            int viewportEndX = existingPlayer.chunkX + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                            int viewportEndY = existingPlayer.chunkY + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;

                            if(chunk.chunkX >= viewportStartX && chunk.chunkX <= viewportEndX && chunk.chunkY >= viewportStartY && chunk.chunkY <= viewportEndY) {
                                int existing = map.get(existingPlayer);
                                revisit.add(existingPlayer);

                                if(existing >= es.entityCapPerPlayer) {
                                    continue nextChunk;
                                }
                            }
                        }
                    }

                    int toSpawn = MathUtils.random(es.spawnMin, es.spawnMax);

                    for(ServerTile tile : chunk.tiles) {
                        if(isApplicableBiome(es, tile.biome)) {
                            float chance = MathUtils.random();

                            if(chance <= es.spawnChance) {
                                float worldX = ExpoShared.tileToPos(tile.tileX) + 8f;
                                float worldY = ExpoShared.tileToPos(tile.tileY) + 8f;

                                if(isFarEnoughFromPlayers(worldX, worldY, es.playerDistanceMin)) {
                                    toSpawn--;

                                    ServerEntity spawned = ServerEntityType.typeToEntity(es.spawnType);
                                    spawned.posX = worldX;
                                    spawned.posY = worldY;
                                    spawned.onGeneration(false, tile.biome, new GenerationRandom(tile.chunk.chunkX, tile.chunk.chunkY, es.spawnType));
                                    ServerWorld.get().registerServerEntity(dimension.getDimensionName(), spawned);

                                    if(es.entityCapPerPlayer != -1) {
                                        for(ServerPlayer rev : revisit) {
                                            map.put(rev, map.get(rev) + 1);
                                        }
                                    }

                                    if(toSpawn <= 0) {
                                        break;
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

    private HashMap<ServerPlayer, Integer> getExistingEntityPage(EntitySpawner es) {
        LinkedList<ServerEntity> existing = dimension.getEntityManager().getEntitiesOf(es.spawnType);
        LinkedList<ServerPlayer> players = dimension.getEntityManager().getAllPlayers();
        HashMap<ServerPlayer, Integer> snapshotMap = new HashMap<>();

        for(ServerEntity e : existing) {
            int cx = e.chunkX;
            int cy = e.chunkY;

            for(ServerPlayer player : players) {
                int viewportStartX = player.chunkX - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                int viewportStartY = player.chunkY - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;
                int viewportEndX = player.chunkX + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                int viewportEndY = player.chunkY + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;

                if(cx >= viewportStartX && cx <= viewportEndX && cy >= viewportStartY && cy <= viewportEndY) {
                    // In viewport.
                    int number = 0;
                    Integer existingNumber = snapshotMap.get(player);
                    if(existingNumber != null) number += existingNumber;
                    snapshotMap.put(player, number + 1);
                }
            }
        }

        return snapshotMap;
    }

}