package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.main.logic.world.gen.EntityPopulator;
import dev.michey.expo.server.main.logic.world.gen.GenerationTile;
import dev.michey.expo.server.main.logic.world.gen.Point;
import dev.michey.expo.server.main.logic.world.gen.PoissonDiskSampler;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;
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
    private ServerDimension dimension;

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
            int x = btx + i % 8;
            int y = bty + i / 8;
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

    public void populate() {
        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        HashMap<BiomeType, List<GenerationTile>> biomeMap = new HashMap<>();

        for(int i = 0; i < tiles.length; i++) {
            BiomeType t = tiles[i].biome;
            if(!biomeMap.containsKey(t)) biomeMap.put(t, new LinkedList<>());

            biomeMap.get(t).add(new GenerationTile(
                    t,
                    tiles[i].layer1.length == 1,
                    wx + ExpoShared.tileToPos(i % 8),
                    wy + ExpoShared.tileToPos(i / 8)
            ));
        }

        List<Pair<ServerEntity, Boolean>> postProcessingList = new LinkedList<>();

        for(BiomeType t : biomeMap.keySet()) {
            var populators = dimension.getChunkHandler().getGenSettings().getEntityPopulators(t);
            if(populators == null || populators.size() == 0) continue;

            for(EntityPopulator populator : populators) {
                List<Point> points = new PoissonDiskSampler(0, 0, CHUNK_SIZE, CHUNK_SIZE, populator.poissonDiskSamplerDistance).sample(wx, wy);

                for(Point p : points) {
                    if(dimension.getChunkHandler().getBiome(ExpoShared.posToTile(p.absoluteX), ExpoShared.posToTile(p.absoluteY)) != t) continue;
                    boolean spawn = MathUtils.random() < populator.spawnChance;

                    if(spawn) {
                        int xi = (int) p.x;
                        int yi = (int) p.y;
                        int tIndex = yi / TILE_SIZE * 8 + xi / TILE_SIZE;

                        if(tiles[tIndex].layer1.length == 1 && tiles[tIndex].layer1[0] != -1) {
                            ServerEntity generatedEntity = ServerEntityType.typeToEntity(populator.type);
                            generatedEntity.posX = (int) p.absoluteX;
                            generatedEntity.posY = (int) p.absoluteY;
                            if(populator.asStaticEntity) generatedEntity.setStaticEntity();
                            generatedEntity.onGeneration(false, t);

                            postProcessingList.add(new Pair<>(generatedEntity, false));

                            if(populator.spreadBetweenEntities != null) {
                                boolean spread = MathUtils.random() < populator.spreadChance;

                                if(spread) {
                                    int amount = MathUtils.random(populator.spreadBetweenAmount[0], populator.spreadBetweenAmount[1]);

                                    for(Vector2 v : GenerationUtils.positions(amount, populator.spreadBetweenDistance[0], populator.spreadBetweenDistance[1])) {
                                        float targetX = p.absoluteX + v.x + populator.spreadOffsets[0];
                                        float targetY = p.absoluteY + v.y + populator.spreadOffsets[1];

                                        if(dimension.getChunkHandler().getBiome(ExpoShared.posToTile(targetX), ExpoShared.posToTile(targetY)) == t) {
                                            ServerEntity spreadEntity = ServerEntityType.typeToEntity(populator.spreadBetweenEntities[MathUtils.random(0, populator.spreadBetweenEntities.length - 1)]);
                                            spreadEntity.posX = (int) targetX;
                                            spreadEntity.posY = (int) targetY;
                                            if(populator.spreadAsStaticEntity) spreadEntity.setStaticEntity();
                                            spreadEntity.onGeneration(true, t);

                                            postProcessingList.add(new Pair<>(spreadEntity, false));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        nextEntry: for(var entry : postProcessingList) {
            for(var otherEntry : postProcessingList) {
                if(otherEntry.value) continue;
                if(otherEntry.key.equals(entry.key)) continue;

                float dis = Vector2.dst(entry.key.posX, entry.key.posY, otherEntry.key.posX, otherEntry.key.posY);

                if(dis <= 4) {
                    entry.value = true;
                    continue nextEntry;
                }
            }
        }

        for(var entry : postProcessingList) {
            if(!entry.value) {
                ServerWorld.get().registerServerEntity(dimension.getDimensionName(), entry.key);
            }
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
            int x = btx + i % 8;
            int y = bty + i / 8;

            // Assign biome.
            tile.biome = dimension.getChunkHandler().getBiome(x, y);

            tile.layerTypes = new TileLayerType[3];
            tile.updateLayer0(null);
            tile.updateLayer1(null);
            tile.updateLayer2(null);
        }

        /*
        boolean generateGroundDetail = MathUtils.random() <= 0.075f;

        if(generateGroundDetail) {
            int minX = 1;
            int minY = 1;
            int pop = MathUtils.random(0, DETAIL_LAYERS.length - 1);
            int[] detail = DETAIL_LAYERS[pop];
            int maxX = 7 - detail[0];
            int maxY = 7 - detail[1];
            int startX = MathUtils.random(minX, maxX);
            int startY = MathUtils.random(minY, maxY);
            ServerTile startTile = tiles[tta(startX, startY)];

            if(startTile.biome == BiomeType.PLAINS || startTile.biome == BiomeType.FOREST) {
                List<ServerTile> visitedTiles = new LinkedList<>();

                for(int i = 2; i < detail.length; i += 2) {
                    int xOffset = detail[i    ];
                    int yOffset = detail[i + 1];

                    int targetX = startX + xOffset;
                    int targetY = startY + yOffset;

                    ServerTile nextTile = tiles[tta(targetX, targetY)];

                    if(nextTile.biome == BiomeType.PLAINS || nextTile.biome == BiomeType.FOREST) {
                        nextTile.layer1 = new int[] {-1};
                        visitedTiles.add(nextTile);
                    }
                }

                for(ServerTile visited : visitedTiles) {
                    for(ServerTile n : visited.getNeighbouringTiles()) {
                        n.updateLayer1();
                    }
                }
            }
        }
        */

        if(populate) populate();
    }

    /** To tileArray index. */
    private int tta(int x, int y) {
        return y * 8 + x;
    }

    /** Structure: [0] = SIZE_X, [1] = SIZE_Y, [2-n] = COORDS */
    public static final int[][] DETAIL_LAYERS = new int[][] {
            new int[] {4, 4,
                    0, 0,
                    1, 0,
                    2, 0,
                    1, 1,
                    2, 1,
                    3, 1,
                    1, 2,
                    2, 2,
                    2, 3
            },
            new int[] {4, 4,
                    1, 0,
                    2, 0,
                    0, 1,
                    1, 1,
                    2, 1,
                    3, 1,
                    1, 2,
                    2, 2,
                    3, 2,
                    1, 3
            },
            new int[] {5, 3,
                    3, 0,
                    0, 1,
                    1, 1,
                    2, 1,
                    3, 1,
                    4, 1,
                    2, 2,
                    3, 2,
            },
            new int[] {4, 5,
                    3, 0,
                    2, 1,
                    0, 2,
                    1, 2,
                    2, 2,
                    3, 2,
                    0, 3,
                    1, 3,
                    3, 3,
                    0, 4,
            },
            new int[] {3, 4,
                    0, 0,
                    2, 0,
                    0, 1,
                    1, 2,
                    2, 2,
                    1, 3
            },
            new int[] {5, 3,
                    0, 0,
                    0, 1,
                    1, 1,
                    2, 2,
                    3, 2,
                    4, 1
            },
            new int[] {5, 4,
                    0, 2,
                    1, 0,
                    1, 1,
                    1, 2,
                    1, 3,
                    2, 0,
                    2, 1,
                    3, 1,
                    4, 1,
                    4, 0
            },
            new int[] {5, 4,
                    4, 0,
                    0, 1,
                    1, 1,
                    1, 2,
                    3, 2,
                    4, 3
            },
            new int[] {4, 3,
                    1, 0,
                    2, 0,
                    0, 1,
                    3, 2
            }
    };

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

        tileBasedEntityIdGrid[ty * 8 + tx] = id;
        tileBasedEntityAmount++;
    }

    /** Detaches an entity from a title within the chunk tile structure. */
    public void detachTileBasedEntity(int tx, int ty) {
        tileBasedEntityIdGrid[ty * 8 + tx] = -1;
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
                for(int i : st.layer0) ia.put(i);
                layerArray.put(ia);
            }
        } else if(layer == 1) {
            for(ServerTile st : tiles) {
                JSONArray ia = new JSONArray();
                for(int i : st.layer1) ia.put(i);
                layerArray.put(ia);
            }
        } else if(layer == 2) {
            for(ServerTile st : tiles) {
                JSONArray ia = new JSONArray();
                for(int i : st.layer2) ia.put(i);
                layerArray.put(ia);
            }
        }

        return layerArray;
    }

    private void arrayToLayer(int layer, JSONArray array) {
        for(int i = 0; i < array.length(); i++) {
            JSONArray idArray = array.getJSONArray(i);

            int[] insert = new int[idArray.length()];

            for(int j = 0; j < idArray.length(); j++) {
                insert[j] = idArray.getInt(j);
            }

            if(layer == 0) {
                tiles[i].layer0 = insert;
            } else if(layer == 1) {
                tiles[i].layer1 = insert;
            } else if(layer == 2) {
                tiles[i].layer2 = insert;
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
