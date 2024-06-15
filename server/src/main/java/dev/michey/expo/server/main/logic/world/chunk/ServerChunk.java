package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.EntityOperation;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.gen.*;
import dev.michey.expo.server.util.EntityMetadata;
import dev.michey.expo.server.util.EntityMetadataMapper;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.*;

public class ServerChunk {

    /** Chunk belongs to this dimension */
    private final ServerDimension dimension;

    /** Chunk data */
    public final int chunkX;
    public final int chunkY;
    private final String chunkKey;
    public final ServerTile[] tiles;
    public long lastTileUpdate;
    public boolean ready;

    /** Tile based entities */
    private int[] tileBasedEntityIdGrid;
    private boolean hasTileBasedEntities = false;
    private int tileBasedEntityAmount;
    public final Object tileEntityLock = new Object();

    /** Inactive chunk properties */
    private List<ServerEntity> inactiveEntities;
    private List<ServerEntity> removeWithoutCachingList;

    /** Water spread logic */
    public boolean checkWaterSpread;
    private final ArrayList<WaterSpreadMemory> tickWaterSpreadList;
    private final HashSet<ServerTile> spreadQueue;
    private static final long WATER_SPREAD_COOLDOWN = 500L;

    public ServerChunk(ServerDimension dimension, int chunkX, int chunkY) {
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkKey = chunkX + "," + chunkY;

        tiles = new ServerTile[ROW_TILES * ROW_TILES];

        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);
        int btx = ExpoShared.posToTile(wx);
        int bty = ExpoShared.posToTile(wy);

        for(int i = 0; i < tiles.length; i++) {
            int x = btx + i % ROW_TILES;
            int y = bty + i / ROW_TILES;
            tiles[i] = new ServerTile(this, x, y, i);
            dimension.getChunkHandler().addTile(tiles[i]);
        }

        lastTileUpdate = System.currentTimeMillis();
        tickWaterSpreadList = new ArrayList<>();
        spreadQueue = new HashSet<>();
    }

    public void doWaterSpreadCheck() {
        ArrayList<ServerTile> waterTiles = new ArrayList<>(tiles.length);

        // Grab water candidates
        for(ServerTile tile : tiles) {
            TileLayerType t2 = tile.dynamicTileParts[2].emulatingType;

            if(TileLayerType.isWater(t2)) {
                waterTiles.add(tile);
            }
        }

        // Go through each water tile and check for valid neighbours
        long now = System.currentTimeMillis();

        for(ServerTile wt : waterTiles) {
            ServerTile[] neighbours = wt.getNeighbouringTilesNESW();

            for(ServerTile n : neighbours) {
                if(n == null) continue;
                if(n.dynamicTileParts == null) continue;
                TileLayerType tlt = n.dynamicTileParts[0].emulatingType;

                if(tlt == TileLayerType.SOIL_HOLE) {
                    // Can spread to this tile.
                    tickWaterSpreadList.add(new WaterSpreadMemory(n, WATER_SPREAD_COOLDOWN + now));
                }
            }
        }
    }

    public void tickWaterSpread() {
        spreadQueue.clear();
        long now = System.currentTimeMillis();

        Iterator<WaterSpreadMemory> wsmIterator = tickWaterSpreadList.iterator();

        while(wsmIterator.hasNext()) {
            WaterSpreadMemory wsm = wsmIterator.next();

            if(wsm.cooldown() > now) {
                break;
            }

            ServerTile st = wsm.destinationTile();

            if(st == null) {
                wsmIterator.remove();
                continue;
            }

            // Spread now.
            wsmIterator.remove();

            if(!TileLayerType.isWater(st.dynamicTileParts[2].emulatingType)) {
                st.updateLayer0(TileLayerType.SOIL_WATERLOGGED);
                st.updateLayer1(TileLayerType.SOIL_WATERLOGGED);
                st.updateLayer2(TileLayerType.WATER_OVERLAY);
                ServerPackets.p50TileFullUpdate(st);
                ServerPackets.p51PositionalSoundAdvanced("water_generic",
                        ExpoShared.tileToPos(st.tileX), ExpoShared.tileToPos(st.tileY), PLAYER_AUDIO_RANGE, 0.25f, PacketReceiver.whoCanSee(st));

                ServerTile[] gnt = st.getNeighbouringTiles();

                for(int i = 0; i < gnt.length; i++) {
                    ServerTile nt = gnt[i];
                    if(nt == null) continue;

                    nt.updateLayer0Adjacent(false);
                    nt.updateLayer1Adjacent(false);
                    nt.updateLayer2Adjacent(false);
                    ServerPackets.p50TileFullUpdate(nt);

                    // modulo operation here because it should only spread to N, E, S, W and not diagonally
                    if(i % 2 == 1 && nt.dynamicTileParts[0].emulatingType == TileLayerType.SOIL_HOLE) {
                        spreadQueue.add(nt);
                    }
                }
            }
        }

        for(ServerTile toAdd : spreadQueue) {
            tickWaterSpreadList.add(new WaterSpreadMemory(toAdd, System.currentTimeMillis() + WATER_SPREAD_COOLDOWN));
        }
    }

    public String getChunkKey() {
        return chunkKey;
    }

    public BiomeType biomeAt(int x, int y) {
        return dimension.getChunkHandler().getBiome(x, y);
    }

    public ServerDimension getDimension() {
        return dimension;
    }

    private Pair<Boolean, float[]> causesCollision(float x, float y, EntityBoundsEntry entry, HashMap<String, List<float[]>> map, float chunkWorldX, float chunkWorldY) {
        boolean collision = false;
        float[] expectedBounds = null;

        if(entry != null) {
            expectedBounds = entry.toWorld(x, y);

            /*
            This fixes the multithreaded intersection entities, but it makes the chunks look less dense

            if(expectedBounds[0] < chunkWorldX || expectedBounds[2] > (chunkWorldX + CHUNK_SIZE) || expectedBounds[1] < chunkWorldY || expectedBounds[3] > (chunkWorldY + CHUNK_SIZE)) {
                collision = true;
            }
            */

            exit: for(List<float[]> coordList : map.values()) {
                for(float[] coords : coordList) {
                    if(ExpoShared.overlap(coords, expectedBounds)) {
                        collision = true;
                        break exit;
                    }
                }
            }
        }

        return new Pair<>(collision, expectedBounds);
    }

    public LinkedList<ServerTile> getBorderingTiles() {
        LinkedList<ServerTile> list = new LinkedList<>();

        int baseX = ExpoShared.posToTile(ExpoShared.chunkToPos(chunkX)) - 1;
        int baseY = ExpoShared.posToTile(ExpoShared.chunkToPos(chunkY)) - 1;

        // Bottom line
        for(int i = 0; i < ROW_TILES + 2; i++) {
            ServerTile _t = dimension.getChunkHandler().getTile(baseX + i, baseY);
            if(_t != null) list.add(_t);
        }

        // Top line
        for(int i = 0; i < ROW_TILES + 2; i++) {
            ServerTile _t = dimension.getChunkHandler().getTile(baseX + i, baseY + ROW_TILES + 2);
            if(_t != null) list.add(_t);
        }

        // Left side line
        for(int i = 0; i < ROW_TILES - 2; i++) {
            ServerTile _t = dimension.getChunkHandler().getTile(baseX, baseY + 1 + i);
            if(_t != null) list.add(_t);
        }

        // Right side line
        for(int i = 0; i < ROW_TILES - 2; i++) {
            ServerTile _t = dimension.getChunkHandler().getTile(baseX + ROW_TILES + 2, baseY + 1 + i);
            if(_t != null) list.add(_t);
        }

        return list;
    }

    private void addToList(EntityBoundsEntry entry, ServerEntity entity, HashMap<String, List<float[]>> map) {
        if(entry != null) {
            String key = entity.chunkX + "," + entity.chunkY;
            if(!map.containsKey(key)) map.put(key, new LinkedList<>());
            float[] set = entry.toWorld(entity.posX, entity.posY);
            map.get(key).add(set);
        }
    }

    private void addToList(float[] entry, ServerEntity entity, HashMap<String, List<float[]>> map) {
        if(entry != null) {
            String key = entity.chunkX + "," + entity.chunkY;
            if(!map.containsKey(key)) map.put(key, new LinkedList<>());
            map.get(key).add(entry);
        }
    }

    public void populate() {
        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        HashSet<BiomeType> biomeSet = new HashSet<>();
        for(ServerTile tile : tiles) biomeSet.add(tile.biome);

        // Generate snapshot of existing entities
        // Format: <chunkKey>:<List<2 corner points>>
        HashMap<String, List<float[]>> existingEntityDimensionMap = new HashMap<>();
        List<ServerEntity> registerMap = new LinkedList<>();
        int acceptChunkXMin = chunkX - 1, acceptChunkXMax = chunkX + 1;
        int acceptChunkYMin = chunkY - 1, acceptChunkYMax = chunkY + 1;

        /*
        for(ServerEntity entity : dimension.getEntityManager().getAllEntities()) {
            if(entity.chunkX >= acceptChunkXMin && entity.chunkX <= acceptChunkXMax && entity.chunkY >= acceptChunkYMin && entity.chunkY <= acceptChunkYMax) {
                addToList(EntityMetadataMapper.get().getFor(entity.getEntityType()).getPopulationBbox(), entity, existingEntityDimensionMap);
            }
        }
        */

        // We are going through this list too as multiple chunks get populated per tick and every generated entity will get added next tick (they are invisible in the list above)
        for(EntityOperation operation : dimension.getEntityManager().getEntityOperationQueue()) {
            if(operation.add) {
                if(operation.payload.chunkX >= acceptChunkXMin && operation.payload.chunkX <= acceptChunkXMax && operation.payload.chunkY >= acceptChunkYMin && operation.payload.chunkY <= acceptChunkYMax) {
                    EntityMetadata meta = EntityMetadataMapper.get().getFor(operation.payload.getEntityType());

                    if(meta != null) {
                        addToList(meta.getPopulationBbox(), operation.payload, existingEntityDimensionMap);
                    }
                }
            }
        }

        for(BiomeType t : biomeSet) {
            var tilePopulators = dimension.getChunkHandler().getGenSettings().getTilePopulators(t);

            if(tilePopulators != null) {
                for(TilePopulator populator : tilePopulators) {
                    GenerationRandom rnd = new GenerationRandom(chunkX, chunkY, populator.type);

                    if(populator.skipChunkChance == 0 || rnd.random() > populator.skipChunkChance) {
                        for(int i = 0; i < tiles.length; i++) {
                            ServerTile tile = tiles[i];
                            int _x = i % ROW_TILES;
                            int _y = i / ROW_TILES;

                            if((_x % populator.skip) == 0 && (_y % populator.skip) == 0) {
                                boolean spawn = rnd.randomD() < populator.chance;

                                if(spawn) {
                                    float proposedX = ExpoShared.tileToPos(tile.tileX) + 8 + rnd.random(populator.spawnOffsets[0], populator.spawnOffsets[1]);
                                    float proposedY = ExpoShared.tileToPos(tile.tileY) + 8 + rnd.random(populator.spawnOffsets[2], populator.spawnOffsets[3]);

                                    int proposedChunkX = ExpoShared.posToChunk(proposedX);
                                    int proposedChunkY = ExpoShared.posToChunk(proposedY);

                                    if(proposedChunkX == chunkX && proposedChunkY == chunkY) {
                                        int proposedTileX = ExpoShared.posToTile(proposedX);
                                        int proposedTileY = ExpoShared.posToTile(proposedY);
                                        ServerTile proposedTile = getDimension().getChunkHandler().getTile(proposedTileX, proposedTileY);

                                        if(proposedTile != null && proposedTile.biome == t) {
                                            EntityBoundsEntry entry = EntityMetadataMapper.get().getFor(populator.type).getPopulationBbox();
                                            var check = causesCollision(proposedX, proposedY, entry, existingEntityDimensionMap, wx, wy);

                                            if(!check.key) {
                                                boolean borderCheckA = populator.borderRequirement == null; // True = has no requirement
                                                boolean borderCheckB = populator.borderRequirementAny == null;
                                                ServerTile[] neighbouringTiles = null;

                                                if(!borderCheckA) {
                                                    neighbouringTiles = tile.getNeighbouringTiles();
                                                    borderCheckA = populator.borderRequirement.meetsRequirements(populator.borderRequirement, neighbouringTiles);
                                                }

                                                if(!borderCheckB) {
                                                    if(neighbouringTiles == null) neighbouringTiles = tile.getNeighbouringTiles();
                                                    borderCheckB = populator.borderRequirementAny.meetsRequirementsAny(populator.borderRequirementAny, populator.borderRequirementAnyCount, neighbouringTiles);
                                                }

                                                if(borderCheckA && borderCheckB) {
                                                    ServerEntity generatedEntity = ServerEntityType.typeToEntity(populator.type);
                                                    generatedEntity.posX = (int) proposedX;
                                                    generatedEntity.posY = (int) proposedY;
                                                    if(populator.asStaticEntity) generatedEntity.setStaticEntity();
                                                    generatedEntity.onGeneration(false, t, rnd);

                                                    registerMap.add(generatedEntity);

                                                    if(populator.spreadData != null) {
                                                        SpreadData sd = populator.pickSpreadData(rnd);

                                                        if(!sd.spreadIgnoreOriginBounds) {
                                                            addToList(check.value, generatedEntity, existingEntityDimensionMap);
                                                        }

                                                        if(rnd.randomD() < sd.spreadChance) {
                                                            doSpread(sd, rnd, registerMap, t, existingEntityDimensionMap, proposedX, proposedY, entry, wx, wy);
                                                        }

                                                        if(sd.spreadIgnoreOriginBounds) {
                                                            addToList(check.value, generatedEntity, existingEntityDimensionMap);
                                                        }
                                                    } else {
                                                        addToList(check.value, generatedEntity, existingEntityDimensionMap);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            var populators = dimension.getChunkHandler().getGenSettings().getEntityPopulators(t);
            if(populators == null || populators.isEmpty()) continue;

            for(EntityPopulator populator : populators) {
                GenerationRandom rnd = new GenerationRandom(chunkX, chunkY, populator.type);
                List<Point> points = new PoissonDiskSampler(0, 0, CHUNK_SIZE, CHUNK_SIZE, populator.poissonDiskSamplerDistance).sample(wx, wy, rnd);

                for(Point p : points) {
                    if(biomeAt(ExpoShared.posToTile(p.absoluteX), ExpoShared.posToTile(p.absoluteY)) != t) continue;
                    boolean spawn = rnd.random() < populator.spawnChance;

                    if(spawn) {
                        EntityBoundsEntry entry = EntityMetadataMapper.get().getFor(populator.type).getPopulationBbox();
                        var check = causesCollision(p.absoluteX, p.absoluteY, entry, existingEntityDimensionMap, wx, wy);

                        if(!check.key) {
                            int xi = (int) p.x;
                            int yi = (int) p.y;
                            int tIndex = yi / TILE_SIZE * ROW_TILES + xi / TILE_SIZE;

                            if(tiles[tIndex].dynamicTileParts[1].layerIds.length == 1 && tiles[tIndex].dynamicTileParts[1].layerIds[0] != -1) {
                                ServerEntity generatedEntity = ServerEntityType.typeToEntity(populator.type);
                                generatedEntity.posX = (int) p.absoluteX;
                                generatedEntity.posY = (int) p.absoluteY;
                                if(populator.asStaticEntity) generatedEntity.setStaticEntity();
                                generatedEntity.onGeneration(false, t, rnd);

                                registerMap.add(generatedEntity);

                                if(populator.spreadData != null) {
                                    SpreadData sd = populator.pickSpreadData(rnd);

                                    if(rnd.randomD() < sd.spreadChance) {
                                        doSpread(sd, rnd, registerMap, t, existingEntityDimensionMap, p.absoluteX, p.absoluteY, entry, wx, wy);
                                    }
                                }

                                addToList(check.value, generatedEntity, existingEntityDimensionMap);
                            }
                        }
                    }
                }
            }
        }

        for(ServerEntity entity : registerMap) {
            ServerWorld.get().registerServerEntity(dimension.getDimensionName(), entity);
        }
    }

    private void doSpread(SpreadData spreadData,
                          GenerationRandom rnd,
                          List<ServerEntity> registerMap,
                          BiomeType t,
                          HashMap<String, List<float[]>> existingEntityDimensionMap,
                          float ax,
                          float ay,
                          EntityBoundsEntry entry,
                          float wx,
                          float wy) {
        if(spreadData == null) return;

        int amount = rnd.random(spreadData.spreadBetweenAmount[0], spreadData.spreadBetweenAmount[1]);

        if(spreadData.spreadUseNextTarget) {
            int remaining = amount;
            float nextOriginX = ax;
            float nextOriginY = ay;

            while(remaining > 0) {
                Vector2 v = rnd.circularRandom(rnd.random(spreadData.spreadBetweenDistance[0], spreadData.spreadBetweenDistance[1]));
                float targetX = nextOriginX + v.x;
                float targetY = nextOriginY + v.y;

                if(biomeAt(ExpoShared.posToTile(targetX), ExpoShared.posToTile(targetY)) == t) {
                    int proposedChunkX = ExpoShared.posToChunk(targetX);
                    int proposedChunkY = ExpoShared.posToChunk(targetY);

                    if(proposedChunkX == chunkX && proposedChunkY == chunkY) {
                        var check = causesCollision(targetX, targetY, entry, existingEntityDimensionMap, wx, wy);

                        if(!check.key) {
                            ServerEntity spreadEntity = ServerEntityType.typeToEntity(spreadData.spreadBetweenEntities[rnd.random(0, spreadData.spreadBetweenEntities.length - 1)]);
                            spreadEntity.posX = (int) targetX;
                            spreadEntity.posY = (int) targetY;
                            if(spreadData.spreadAsStaticEntity) spreadEntity.setStaticEntity();
                            spreadEntity.onGeneration(true, t, rnd);

                            addToList(check.value, spreadEntity, existingEntityDimensionMap);
                            registerMap.add(spreadEntity);

                            nextOriginX = spreadEntity.posX;
                            nextOriginY = spreadEntity.posY;
                        }
                    }
                }

                remaining--;
            }
        } else {
            for(Vector2 v : rnd.positions(amount, spreadData.spreadBetweenDistance[0], spreadData.spreadBetweenDistance[1])) {
                float targetX = ax + v.x + spreadData.spreadOffsets[0];
                float targetY = ay + v.y + spreadData.spreadOffsets[1];

                if(biomeAt(ExpoShared.posToTile(targetX), ExpoShared.posToTile(targetY)) == t) {
                    int proposedChunkX = ExpoShared.posToChunk(targetX);
                    int proposedChunkY = ExpoShared.posToChunk(targetY);

                    if(proposedChunkX == chunkX && proposedChunkY == chunkY) {
                        var check = causesCollision(targetX, targetY, entry, existingEntityDimensionMap, wx, wy);

                        if(!check.key) {
                            ServerEntity spreadEntity = ServerEntityType.typeToEntity(spreadData.spreadBetweenEntities[rnd.random(0, spreadData.spreadBetweenEntities.length - 1)]);
                            spreadEntity.posX = (int) targetX;
                            spreadEntity.posY = (int) targetY;
                            if(spreadData.spreadAsStaticEntity) spreadEntity.setStaticEntity();
                            spreadEntity.onGeneration(true, t, rnd);

                            addToList(check.value, spreadEntity, existingEntityDimensionMap);
                            registerMap.add(spreadEntity);
                        }
                    }
                }
            }
        }
    }

    public void generate(boolean populate) {
        checkWaterSpread = true;

        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        int btx = ExpoShared.posToTile(wx);
        int bty = ExpoShared.posToTile(wy);

        for(int i = 0; i < tiles.length; i++) {
            ServerTile tile = tiles[i];
            int x = btx + i % ROW_TILES;
            int y = bty + i / ROW_TILES;

            // Assign biome.
            tile.biome = biomeAt(x, y);

            tile.dynamicTileParts = new DynamicTilePart[] {new DynamicTilePart(), new DynamicTilePart(), new DynamicTilePart()};
            tile.updateLayer0(null);
            tile.updateLayer1(null);
            tile.updateLayer2(null);
        }

        LinkedList[] postProcessedLists = new LinkedList[3];

        for(NoisePostProcessor npp : dimension.getChunkHandler().getGenSettings().getNoiseSettings().postProcessList) {
            if(npp.postProcessorLogic instanceof PostProcessorLayer ppl) {
                for(int i = 0; i < tiles.length; i++) {
                    ServerTile tile = tiles[i];
                    int x = btx + i % ROW_TILES;
                    int y = bty + i / ROW_TILES;

                    Pair<TileLayerType, Integer> returnedType = ppl.getLayerType(
                            tile.dynamicTileParts[ppl.processLayer].emulatingType,
                            dimension.getChunkHandler().normalized(dimension.getChunkHandler().getNoisePostProcessorMap().get(ppl.noiseName), x, y)
                    );

                    if(returnedType != null) {
                        tile.updateLayer(returnedType.value, returnedType.key);

                        if(returnedType.value == 0) {
                            tile.updateLayer1(TileLayerType.EMPTY);

                            if(postProcessedLists[1] == null) {
                                postProcessedLists[1] = new LinkedList();
                            }

                            postProcessedLists[1].add(tile);
                        }

                        if(postProcessedLists[returnedType.value] == null) {
                            postProcessedLists[returnedType.value] = new LinkedList();
                        }

                        postProcessedLists[returnedType.value].add(tile);
                    }
                }
            }
        }

        for(int i = 0; i < postProcessedLists.length; i++) {
            LinkedList<ServerTile> list = postProcessedLists[i];
            if(list == null) continue;

            for(ServerTile st : list) {
                for(ServerTile neighbours : st.getNeighbouringTiles()) {
                    if(neighbours == null) continue;
                    if(neighbours.updateLayerAdjacent(i, false)) {
                        ServerPackets.p32ChunkDataSingle(neighbours, i);
                    }
                }
            }
        }

        if(populate) populate();
    }

    /** Called when the chunk has been inactive before and is now marked as active again. */
    public void onActive() {
        // log(chunkKey + " ACTIVE, re-adding " + inactiveEntities.size() + " entities");

        for(ServerEntity wasInactive : inactiveEntities) {
            dimension.getEntityManager().addEntitySafely(wasInactive);
        }

        inactiveEntities.clear();
    }

    /** Called when the chunk has been active before and is now marked as inactive. */
    public void onInactive() {
        if(inactiveEntities == null) {
            inactiveEntities = new LinkedList<>();
            removeWithoutCachingList = new LinkedList<>();
        }

        for(ServerEntity serverEntity : dimension.getEntityManager().getAllEntities()) {
            if(serverEntity.getEntityType() == ServerEntityType.PLAYER) continue;
            if(serverEntity.chunkX == chunkX && serverEntity.chunkY == chunkY) {
                if(serverEntity.persistentEntity) {
                    inactiveEntities.add(serverEntity);
                } else {
                    removeWithoutCachingList.add(serverEntity);
                }
            }
        }

        for(ServerEntity nowInactive : inactiveEntities) {
            if(nowInactive.getEntityType() == ServerEntityType.PLAYER) continue;
            dimension.getEntityManager().removeEntitySafely(nowInactive);
        }

        for(ServerEntity nowInactive : removeWithoutCachingList) {
            dimension.getEntityManager().removeEntitySafely(nowInactive);
        }

        removeWithoutCachingList.clear();
        // log(chunkKey + " INACTIVE, removed " + inactiveEntities.size() + " entities");
    }

    /** Called when the chunk has been inactive before and is now commanded to save. */
    public void onSave(ExecutorService saveWith, boolean shutdown) {
        //log(chunkKey + " SAVE, saving " + inactiveEntities.size() + " entities");
        String worldName = ExpoServerBase.get().getWorldSaveHandler().getWorldName();
        if(worldName.startsWith("dev-world-")) return;

        Path path = getChunkFile().toPath();
        saveWith.execute(() -> _onSave(path, shutdown));
    }

    protected void _onSave(Path path, boolean shutdown) {
        if(!shutdown) {
            // Free up memory
            for(ServerTile tile : tiles) {
                dimension.getChunkHandler().removeNoiseCache(tile.tileX, tile.tileY);
                dimension.getChunkHandler().removeTile(tile.tileX, tile.tileY);
            }
        }

        save(path);
    }

    /** Attaches an entity to a tile within the chunk tile structure. */
    public void attachTileBasedEntity(int id, int tx, int ty) {
        synchronized (tileEntityLock) {
            if(!hasTileBasedEntities) {
                hasTileBasedEntities = true;
                tileBasedEntityIdGrid = new int[ROW_TILES * ROW_TILES];
                Arrays.fill(tileBasedEntityIdGrid, -1);
            }

            tileBasedEntityIdGrid[ty * ROW_TILES + tx] = id;
            tileBasedEntityAmount++;
        }
    }

    /** Detaches an entity from a title within the chunk tile structure. */
    public void detachTileBasedEntity(int tx, int ty) {
        synchronized (tileEntityLock) {
            tileBasedEntityIdGrid[ty * ROW_TILES + tx] = -1;
            tileBasedEntityAmount--;

            if(tileBasedEntityAmount == 0) {
                hasTileBasedEntities = false;
            }
        }
    }

    public void loadFromFile() {
        // log("Loading " + chunkIdentifier());
        String loadedString = null;

        try {
            loadedString = Files.readString(getChunkFile().toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(loadedString == null) {
            log("Failed to load " + chunkIdentifier() + " from file, regenerating instead");
            generate(true);
        } else {
            try {
                JSONObject object = new JSONObject(loadedString);

                {
                    // biomeData
                    JSONObject biomeData = object.getJSONObject("biomeData");
                        JSONArray biomesArray = biomeData.getJSONArray("biomes");
                        for(int i = 0; i < tiles.length; i++) tiles[i].biome = BiomeType.idToBiome(biomesArray.getInt(i));

                        JSONArray tileTypesArray = biomeData.getJSONArray("layerTypes");
                        for(int i = 0; i < tiles.length; i++) {
                            tiles[i].dynamicTileParts = new DynamicTilePart[] {new DynamicTilePart(), new DynamicTilePart(), new DynamicTilePart()};
                            JSONArray _a = tileTypesArray.getJSONArray(i);
                            for(int j = 0; j < _a.length(); j++) {
                                tiles[i].dynamicTileParts[j].update(TileLayerType.values()[_a.getInt(j)]);
                            }
                        }

                        arrayToLayer(0, biomeData.getJSONArray("layer0"));
                        arrayToLayer(1, biomeData.getJSONArray("layer1"));
                        arrayToLayer(2, biomeData.getJSONArray("layer2"));
                }

                {
                    // entityData
                    JSONArray entityData = object.getJSONArray("entityData");

                    for(int i = 0; i < entityData.length(); i++) {
                        JSONObject entityObject = entityData.getJSONObject(i);
                        ServerEntity converted = SavableEntity.entityFromSavable(entityObject, this);
                        dimension.getEntityManager().addEntitySafely(converted);
                    }
                }
            } catch (JSONException e) {
                log("Failed to convert save file of " + chunkIdentifier() + " to json, regenerating instead");
                e.printStackTrace();
                generate(true);
            }
        }
    }

    public void save(Path path) {
        try {
            //log("Saving " + chunkIdentifier() + " -> " + inactiveEntities.size() + " inactive entities to save");
            Files.writeString(path, chunkToSavableString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log("Failed to save " + chunkIdentifier());
            e.printStackTrace();
        }
    }

    private String chunkToSavableString() {
        JSONObject object = new JSONObject();

        {
            // biomeData
            JSONObject biomeData = new JSONObject();
                JSONArray biomesArray = new JSONArray();
                for(ServerTile tile : tiles) biomesArray.put(tile.biome.BIOME_ID);
                biomeData.put("biomes", biomesArray);

                JSONArray layerTypesArray = new JSONArray();
                for(ServerTile tile : tiles) {
                    JSONArray _a = new JSONArray();
                    for(DynamicTilePart tlt : tile.dynamicTileParts) _a.put(tlt.emulatingType.SERIALIZATION_ID);
                    layerTypesArray.put(_a);
                }
                biomeData.put("layerTypes", layerTypesArray);

                biomeData.put("layer0", layerToArray(0));
                biomeData.put("layer1", layerToArray(1));
                biomeData.put("layer2", layerToArray(2));

            // pack
            object.put("biomeData", biomeData);
        }

        {
            // entityData
            JSONArray entityData = new JSONArray();

            // for each tile entity
            for(ServerEntity inactiveEntity : inactiveEntities) {
                SavableEntity savableEntity = inactiveEntity.onSave();
                if(savableEntity == null) continue;
                //ExpoLogger.log("saving " + chunkKey + ": " + savableEntity.packaged.toString());
                entityData.put(savableEntity.packaged);
            }

            // pack
            object.put("entityData", entityData);
        }

        return object.toString();
    }

    private JSONArray layerToArray(int layer) {
        JSONArray layerArray = new JSONArray();

        if(layer == 0) {
            for(ServerTile st : tiles) {
                JSONArray ia = new JSONArray();
                for(int i : st.dynamicTileParts[0].layerIds) ia.put(i);
                layerArray.put(ia);
            }
        } else if(layer == 1) {
            for(ServerTile st : tiles) {
                JSONArray ia = new JSONArray();
                for(int i : st.dynamicTileParts[1].layerIds) ia.put(i);
                layerArray.put(ia);
            }
        } else if(layer == 2) {
            for(ServerTile st : tiles) {
                JSONArray ia = new JSONArray();
                for(int i : st.dynamicTileParts[2].layerIds) ia.put(i);
                layerArray.put(ia);
            }
        }

        return layerArray;
    }

    public int[] getTileBasedEntityIdGrid() {
        return tileBasedEntityIdGrid;
    }

    public boolean hasTileBasedEntities() {
        return hasTileBasedEntities;
    }

    private void arrayToLayer(int layer, JSONArray array) {
        for(int i = 0; i < array.length(); i++) {
            JSONArray idArray = array.getJSONArray(i);

            int[] insert = new int[idArray.length()];

            for(int j = 0; j < idArray.length(); j++) {
                insert[j] = idArray.getInt(j);
            }

            if(layer == 0) {
                tiles[i].dynamicTileParts[0].setTileIds(insert);
            } else if(layer == 1) {
                tiles[i].dynamicTileParts[1].setTileIds(insert);
            } else if(layer == 2) {
                tiles[i].dynamicTileParts[2].setTileIds(insert);
            }
        }
    }

    private String chunkIdentifier() {
        return "ServerChunk [" + chunkX + "," + chunkY + "]";
    }

    public File getChunkFile() {
        String path = ExpoServerBase.get().getWorldSaveHandler().getPathDimensionSpecificFolder(dimension.getDimensionName());
        return new File(path + File.separator + chunkX + "," + chunkY);
    }

}