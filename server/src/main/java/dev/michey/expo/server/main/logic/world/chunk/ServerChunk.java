package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
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
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;
import make.some.noise.Noise;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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

    /** Tile based entities */
    private int[] tileBasedEntityIdGrid;
    private boolean hasTileBasedEntities = false;
    private int tileBasedEntityAmount;

    /** Inactive chunk properties */
    private List<ServerEntity> inactiveEntities;

    public ServerChunk(ServerDimension dimension, int chunkX, int chunkY) {
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkKey = chunkX + "," + chunkY;

        int totalTiles = ROW_TILES * ROW_TILES;

        tiles = new ServerTile[totalTiles];

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

    private Pair<Boolean, float[]> causesCollision(float x, float y, EntityBoundsEntry entry, HashMap<String, List<float[]>> map) {
        boolean collision = false;
        float[] expectedBounds = null;

        if(entry != null) {
            expectedBounds = entry.toWorld(x, y);

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

        List<BiomeType> biomeList = new LinkedList<>();

        for(ServerTile tile : tiles) {
            BiomeType t = tile.biome;
            if(!biomeList.contains(t)) biomeList.add(t);
        }

        // Generate snapshot of existing entities
        // Format: <chunkKey>:<List<2 corner points>>
        HashMap<String, List<float[]>> existingEntityDimensionMap = new HashMap<>();
        List<ServerEntity> registerMap = new LinkedList<>();
        int acceptChunkXMin = chunkX - 1, acceptChunkXMax = chunkX + 1;
        int acceptChunkYMin = chunkY - 1, acceptChunkYMax = chunkY + 1;

        for(ServerEntity entity : dimension.getEntityManager().getAllEntities()) {
            if(entity.chunkX >= acceptChunkXMin && entity.chunkX <= acceptChunkXMax && entity.chunkY >= acceptChunkYMin && entity.chunkY <= acceptChunkYMax) {
                addToList(EntityPopulationBounds.get().getFor(entity.getEntityType()), entity, existingEntityDimensionMap);
            }
        }

        // We are going through this list too as multiple chunks get populated per tick and every generated entity will get added next tick (they are invisible in the list above)
        for(EntityOperation operation : dimension.getEntityManager().getEntityOperationQueue()) {
            if(operation.add) {
                if(operation.payload.chunkX >= acceptChunkXMin && operation.payload.chunkX <= acceptChunkXMax && operation.payload.chunkY >= acceptChunkYMin && operation.payload.chunkY <= acceptChunkYMax) {
                    addToList(EntityPopulationBounds.get().getFor(operation.payload.getEntityType()), operation.payload, existingEntityDimensionMap);
                }
            }
        }

        for(BiomeType t : biomeList) {
            var populators = dimension.getChunkHandler().getGenSettings().getEntityPopulators(t);
            if(populators == null || populators.size() == 0) continue;

            for(EntityPopulator populator : populators) {
                List<Point> points = new PoissonDiskSampler(0, 0, CHUNK_SIZE, CHUNK_SIZE, populator.poissonDiskSamplerDistance).sample(wx, wy);

                for(Point p : points) {
                    if(dimension.getChunkHandler().getBiome(ExpoShared.posToTile(p.absoluteX), ExpoShared.posToTile(p.absoluteY)) != t) continue;
                    boolean spawn = MathUtils.random() < populator.spawnChance;

                    if(spawn) {
                        EntityBoundsEntry entry = EntityPopulationBounds.get().getFor(populator.type);
                        var check = causesCollision(p.absoluteX, p.absoluteY, entry, existingEntityDimensionMap);

                        if(!check.key) {
                            int xi = (int) p.x;
                            int yi = (int) p.y;
                            int tIndex = yi / TILE_SIZE * ROW_TILES + xi / TILE_SIZE;

                            if(tiles[tIndex].dynamicTileParts[1].layerIds.length == 1 && tiles[tIndex].dynamicTileParts[1].layerIds[0] != -1) {
                                ServerEntity generatedEntity = ServerEntityType.typeToEntity(populator.type);
                                generatedEntity.posX = (int) p.absoluteX;
                                generatedEntity.posY = (int) p.absoluteY;
                                if(populator.asStaticEntity) generatedEntity.setStaticEntity();
                                generatedEntity.onGeneration(false, t);

                                addToList(check.value, generatedEntity, existingEntityDimensionMap);
                                registerMap.add(generatedEntity);

                                if(populator.spreadBetweenEntities != null) {
                                    boolean spread = MathUtils.random() < populator.spreadChance;

                                    if(spread) {
                                        int amount = MathUtils.random(populator.spreadBetweenAmount[0], populator.spreadBetweenAmount[1]);

                                        if(populator.spreadUseNextTarget) {
                                            int remaining = amount;
                                            float nextOriginX = p.absoluteX;
                                            float nextOriginY = p.absoluteY;

                                            while(remaining > 0) {
                                                Vector2 v = GenerationUtils.circularRandom(MathUtils.random(populator.spreadBetweenDistance[0], populator.spreadBetweenDistance[1]));
                                                float targetX = nextOriginX + v.x;
                                                float targetY = nextOriginY + v.y;

                                                if(dimension.getChunkHandler().getBiome(ExpoShared.posToTile(targetX), ExpoShared.posToTile(targetY)) == t) {
                                                    check = causesCollision(targetX, targetY, entry, existingEntityDimensionMap);

                                                    if(!check.key) {
                                                        ServerEntity spreadEntity = ServerEntityType.typeToEntity(populator.spreadBetweenEntities[MathUtils.random(0, populator.spreadBetweenEntities.length - 1)]);
                                                        spreadEntity.posX = (int) targetX;
                                                        spreadEntity.posY = (int) targetY;
                                                        if(populator.spreadAsStaticEntity) spreadEntity.setStaticEntity();
                                                        spreadEntity.onGeneration(true, t);

                                                        addToList(check.value, spreadEntity, existingEntityDimensionMap);
                                                        registerMap.add(spreadEntity);

                                                        nextOriginX = spreadEntity.posX;
                                                        nextOriginY = spreadEntity.posY;
                                                    }
                                                }

                                                remaining--;
                                            }
                                        } else {
                                            for(Vector2 v : GenerationUtils.positions(amount, populator.spreadBetweenDistance[0], populator.spreadBetweenDistance[1])) {
                                                float targetX = p.absoluteX + v.x + populator.spreadOffsets[0];
                                                float targetY = p.absoluteY + v.y + populator.spreadOffsets[1];

                                                if(dimension.getChunkHandler().getBiome(ExpoShared.posToTile(targetX), ExpoShared.posToTile(targetY)) == t) {
                                                    check = causesCollision(targetX, targetY, entry, existingEntityDimensionMap);

                                                    if(!check.key) {
                                                        ServerEntity spreadEntity = ServerEntityType.typeToEntity(populator.spreadBetweenEntities[MathUtils.random(0, populator.spreadBetweenEntities.length - 1)]);
                                                        spreadEntity.posX = (int) targetX;
                                                        spreadEntity.posY = (int) targetY;
                                                        if(populator.spreadAsStaticEntity) spreadEntity.setStaticEntity();
                                                        spreadEntity.onGeneration(true, t);

                                                        addToList(check.value, spreadEntity, existingEntityDimensionMap);
                                                        registerMap.add(spreadEntity);
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
        }

        for(ServerEntity entity : registerMap) {
            ServerWorld.get().registerServerEntity(dimension.getDimensionName(), entity);
        }

        // log("Population took " + ((System.nanoTime() - start) / 1_000_000.0d) + "ms.");
    }

    public void generate(boolean populate) {
        // log("Generating " + chunkIdentifier());

        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        int btx = ExpoShared.posToTile(wx);
        int bty = ExpoShared.posToTile(wy);

        for(int i = 0; i < tiles.length; i++) {
            ServerTile tile = tiles[i];
            int x = btx + i % ROW_TILES;
            int y = bty + i / ROW_TILES;

            // Assign biome.
            tile.biome = dimension.getChunkHandler().getBiome(x, y);

            tile.dynamicTileParts = new DynamicTilePart[] {new DynamicTilePart(), new DynamicTilePart(), new DynamicTilePart()};
            tile.updateLayer0(null);
            tile.updateLayer1(null);
            tile.updateLayer2(null);
        }

        {
            // Dirt
            Noise p = dimension.getChunkHandler().getNoisePostProcessorMap().get("dirt");

            if(p != null) {
                NoisePostProcessor processor = dimension.getChunkHandler().getGenSettings().getNoiseSettings().postProcessList.get("dirt");
                List<ServerTile> adjust = null;

                for(int i = 0; i < tiles.length; i++) {
                    ServerTile tile = tiles[i];
                    int x = btx + i % ROW_TILES;
                    int y = bty + i / ROW_TILES;

                    float normalized = dimension.getChunkHandler().normalized(p, x, y);
                    boolean isDirt = normalized >= processor.threshold && (tile.dynamicTileParts[1].emulatingType == TileLayerType.FOREST || tile.dynamicTileParts[1].emulatingType == TileLayerType.GRASS);

                    if(isDirt) {
                        tile.updateLayer1(TileLayerType.EMPTY);
                        if(adjust == null) adjust = new LinkedList<>();
                        adjust.add(tile);
                    }
                }

                if(adjust != null) {
                    for(ServerTile tile : adjust) {
                        for(ServerTile neighbours : tile.getNeighbouringTiles()) {
                            if(neighbours == null) continue;

                            if(neighbours.updateLayer1Adjacent()) {
                                ServerPackets.p32ChunkDataSingle(neighbours, 1);
                            }
                        }
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
            wasInactive.trackedVisibility = false;
            dimension.getEntityManager().addEntitySafely(wasInactive);
        }

        inactiveEntities.clear();
    }

    /** Called when the chunk has been active before and is now marked as inactive. */
    public void onInactive() {
        if(inactiveEntities == null) {
            inactiveEntities = new LinkedList<>();
        }

        for(ServerEntity serverEntity : dimension.getEntityManager().getAllEntities()) {
            if(serverEntity.getEntityType() == ServerEntityType.PLAYER) continue;
            if(serverEntity.chunkX == chunkX && serverEntity.chunkY == chunkY) {
                inactiveEntities.add(serverEntity);
            }
        }

        for(ServerEntity nowInactive : inactiveEntities) {
            if(nowInactive.getEntityType() == ServerEntityType.PLAYER) continue;
            dimension.getEntityManager().removeEntitySafely(nowInactive);
        }

        // log(chunkKey + " INACTIVE, removed " + inactiveEntities.size() + " entities");
    }

    /** Called when the chunk has been inactive before and is now commanded to save. */
    public void onSave() {
        // log(chunkKey + " SAVE, saving " + inactiveEntities.size() + " entities");
        String worldName = ExpoServerBase.get().getWorldSaveHandler().getWorldName();
        if(worldName.startsWith("dev-world-")) return;

        for(ServerTile tile : tiles) dimension.getChunkHandler().removeTile(tile.tileX, tile.tileY);
        save();
    }

    /** Attaches an entity to a tile within the chunk tile structure. */
    public void attachTileBasedEntity(int id, int tx, int ty) {
        if(!hasTileBasedEntities) {
            hasTileBasedEntities = true;
            tileBasedEntityIdGrid = new int[ROW_TILES * ROW_TILES];
            Arrays.fill(tileBasedEntityIdGrid, -1);
        }

        tileBasedEntityIdGrid[ty * ROW_TILES + tx] = id;
        tileBasedEntityAmount++;
    }

    /** Detaches an entity from a title within the chunk tile structure. */
    public void detachTileBasedEntity(int tx, int ty) {
        tileBasedEntityIdGrid[ty * ROW_TILES + tx] = -1;
        tileBasedEntityAmount--;

        if(tileBasedEntityAmount == 0) {
            hasTileBasedEntities = false;
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

    public void save() {
        try {
            // log("Saving " + chunkIdentifier());
            Files.writeString(getChunkFile().toPath(), chunkToSavableString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

    private File getChunkFile() {
        String path = ExpoServerBase.get().getWorldSaveHandler().getPathDimensionSpecificFolder(dimension.getDimensionName());
        return new File(path + File.separator + chunkX + "," + chunkY);
    }

}
