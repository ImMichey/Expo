package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.ServerGrass;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class ServerChunk {

    /** Chunk belongs to this dimension */
    private ServerDimension dimension;

    /** Chunk data */
    public final int chunkX;
    public final int chunkY;
    private final String chunkKey;

    public final BiomeType[] biomes;
    public int[][] layer0;
    public int[][] layer1;
    public int[][] layer2;

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

        biomes = new BiomeType[totalTiles];
        layer0 = new int[totalTiles][];
        layer1 = new int[totalTiles][];
        layer2 = new int[totalTiles][];

        Arrays.fill(biomes, BiomeType.VOID);
    }

    public String getChunkKey() {
        return chunkKey;
    }

    private BiomeType biomeAt(int x, int y) {
        return dimension.getChunkHandler().getBiome(x, y);
    }

    public void populate() {
        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        for(int i = 0; i < biomes.length; i++) {
            BiomeType t = biomes[i];
            boolean index = layer1[i].length == 1;

            if(t == BiomeType.GRASS && index && MathUtils.random() <= 0.15f) {
                int x = i % 8;
                int y = i / 8;

                ServerGrass grass = new ServerGrass();
                grass.posX = wx + ExpoShared.tileToPos(x);
                grass.posY = wy + ExpoShared.tileToPos(y);
                grass.setStaticEntity();
                //grass.attachToTile(this, x, y);

                ServerWorld.get().registerServerEntity(dimension.getDimensionName(), grass);

                // Spawn around
                int spawnAround;
                float r = MathUtils.random();

                if(r <= 0.05f) {
                    spawnAround = 8;
                } else if(r <= 0.125f) {
                    spawnAround = 7;
                } else if(r <= 0.25f) {
                    spawnAround = 6;
                } else if(r <= 0.375f) {
                    spawnAround = 5;
                } else if(r <= 0.5f) {
                    spawnAround = 4;
                } else if(r <= 0.75f) {
                    spawnAround = 3;
                } else if(r <= 0.95f) {
                    spawnAround = 2;
                } else if(r <= 0.98) {
                    spawnAround = 1;
                } else {
                    spawnAround = 0;
                }

                for(Vector2 v : GenerationUtils.positions(spawnAround, 8.0f, 14.0f)) {
                    ServerGrass around = new ServerGrass();
                    around.posX = grass.posX + v.x;
                    around.posY = grass.posY + v.y;
                    around.setStaticEntity();

                    ServerWorld.get().registerServerEntity(dimension.getDimensionName(), around);
                }
            }
        }
    }

    public void generate(boolean populate) {
        // log("Generating " + chunkIdentifier());

        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        int btx = ExpoShared.posToTile(wx);
        int bty = ExpoShared.posToTile(wy);

        for(int i = 0; i < layer0.length; i++) layer0[i] = new int[] {-1};
        for(int i = 0; i < layer1.length; i++) layer1[i] = new int[] {-1};
        for(int i = 0; i < layer2.length; i++) layer2[i] = new int[] {-1};

        for(int i = 0; i < biomes.length; i++) {
            int x = btx + i % 8;
            int y = bty + i / 8;
            biomes[i] = dimension.getChunkHandler().getBiome(x, y);

            BiomeType b = biomes[i];
            int[] tileBounds = b.BIOME_LAYER_TEXTURES;

            if(tileBounds != null) {
                int minTile = tileBounds[1];

                // LAYER 0
                int layer0Id = tileBounds[0];
                layer0[i][0] = layer0Id;

                // LAYER 1
                int[] indices = indexStraightDiagonal(b.BIOME_NEIGHBOURS, x, y);
                int tis = indices[0];
                int tid = indices[1];

                if(tis == 0 && tid == 0) {
                    // Special case, no neighbour
                    layer1[i] = new int[] {minTile + 1};
                } else if(tis == 15 && tid == 15) {
                    // Special case, every 8 neighbours are same tile
                    layer1[i] = new int[] {minTile};
                } else if(tis == 15) {
                    // N E S W all valid neighbours (straight)
                    if(tid == 0) {
                        // No diagonal neighbours
                        layer1[i] = new int[] {minTile + 20, minTile + 21, minTile + 18, minTile + 19};
                    } else {
                        // tig between [1-14]
                        boolean northWest = tid / NORTH_WEST == 1;
                        boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
                        boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
                        boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

                        layer1[i] = new int[] {
                                southWest ? minTile + 8 : minTile + 20,
                                southEast ? minTile + 5 : minTile + 21,
                                northWest ? minTile + 14 : minTile + 18,
                                northEast ? minTile + 11 : minTile + 19
                        };
                    }
                } else {
                    // N E S W not all valid neighbours
                    boolean west = tis / WEST == 1;
                    boolean south = (tis % WEST) / SOUTH == 1;
                    boolean east = (tis % WEST % SOUTH) / EAST == 1;
                    boolean north = (tis % WEST % SOUTH % EAST) / NORTH == 1;

                    boolean northWest = tid / NORTH_WEST == 1;
                    boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
                    boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
                    boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

                    int c1, c2, c3, c4;

                    { // Corner Bottom Left
                        if(west && south && southWest) {
                            c1 = minTile + 8;
                        } else if(west && south) {
                            c1 = minTile + 20;
                        } else if(west && southWest) {
                            c1 = minTile + 16;
                        } else if(south && southWest) {
                            c1 = minTile + 4;
                        } else if(west) {
                            c1 = minTile + 16;
                        } else if(south) {
                            c1 = minTile + 4;
                        } else {
                            c1 = minTile + 12;
                        }
                    }

                    { // Corner Bottom Right
                        if(east && south && southEast) {
                            c2 = minTile + 5;
                        } else if(east && south) {
                            c2 = minTile + 21;
                        } else if(east && southEast) {
                            c2 = minTile + 13;
                        } else if(south && southEast) {
                            c2 = minTile + 9;
                        } else if(east) {
                            c2 = minTile + 13;
                        } else if(south) {
                            c2 = minTile + 9;
                        } else {
                            c2 = minTile + 17;
                        }
                    }

                    { // Corner Top Left
                        if(west && north && northWest) {
                            c3 = minTile + 14;
                        } else if(west && north) {
                            c3 = minTile + 18;
                        } else if(west && northWest) {
                            c3 = minTile + 6;
                        } else if(north && northWest) {
                            c3 = minTile + 10;
                        } else if(west) {
                            c3 = minTile + 6;
                        } else if(north) {
                            c3 = minTile + 10;
                        } else {
                            c3 = minTile + 2;
                        }
                    }

                    { // Corner Top Right
                        if(east && north && northEast) {
                            c4 = minTile + 11;
                        } else if(east && north) {
                            c4 = minTile + 19;
                        } else if(east && northEast) {
                            c4 = minTile + 3;
                        } else if(north && northEast) {
                            c4 = minTile + 15;
                        } else if(east) {
                            c4 = minTile + 3;
                        } else if(north) {
                            c4 = minTile + 15;
                        } else {
                            c4 = minTile + 7;
                        }
                    }

                    layer1[i] = new int[] {c1, c2, c3, c4};
                }
            }
        }

        if(populate) populate();
    }

    private final int NORTH = 1;
    private final int EAST = 2;
    private final int SOUTH = 4;
    private final int WEST = 8;

    private final int NORTH_EAST = 1;
    private final int SOUTH_EAST = 2;
    private final int SOUTH_WEST = 4;
    private final int NORTH_WEST = 8;

    private int[] indexStraightDiagonal(String[] acceptedNeighbours, int x, int y) {
        BiomeType[] biomes = new BiomeType[acceptedNeighbours.length];
        for(int i = 0; i < acceptedNeighbours.length; i++) biomes[i] = BiomeType.valueOf(acceptedNeighbours[i]);
        int tis = 0, tid = 0;

        BiomeType n = biomeAt(x, y + 1);
        BiomeType e = biomeAt(x + 1, y);
        BiomeType s = biomeAt(x, y - 1);
        BiomeType w = biomeAt(x - 1, y);

        BiomeType ne = biomeAt(x + 1, y + 1);
        BiomeType se = biomeAt(x + 1, y - 1);
        BiomeType sw = biomeAt(x - 1, y - 1);
        BiomeType nw = biomeAt(x - 1, y + 1);

        for(BiomeType b : biomes) {
            if(n == b) {
                tis += NORTH; break;
            }
        }
        for(BiomeType b : biomes) {
            if(e == b) {
                tis += EAST; break;
            }
        }
        for(BiomeType b : biomes) {
            if(s == b) {
                tis += SOUTH; break;
            }
        }
        for(BiomeType b : biomes) {
            if(w == b) {
                tis += WEST; break;
            }
        }

        for(BiomeType b : biomes) {
            if(ne == b) {
                tid += NORTH_EAST; break;
            }
        }
        for(BiomeType b : biomes) {
            if(se == b) {
                tid += SOUTH_EAST; break;
            }
        }
        for(BiomeType b : biomes) {
            if(sw == b) {
                tid += SOUTH_WEST; break;
            }
        }
        for(BiomeType b : biomes) {
            if(nw == b) {
                tid += NORTH_WEST; break;
            }
        }

        return new int[] {tis, tid};
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
            if(serverEntity.chunkX == chunkX && serverEntity.chunkY == chunkY) {
                inactiveEntities.add(serverEntity);
            }
        }

        for(ServerEntity nowInactive : inactiveEntities) {
            dimension.getEntityManager().removeEntitySafely(nowInactive);
        }

        // log(chunkKey + " INACTIVE, removed " + inactiveEntities.size() + " entities");
    }

    /** Called when the chunk has been inactive before and is now commanded to save. */
    public void onSave() {
        // log(chunkKey + " SAVE, saving " + inactiveEntities.size() + " entities");
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
                        for(int i = 0; i < biomes.length; i++) biomes[i] = BiomeType.idToBiome(biomesArray.getInt(i));

                        arrayToLayer(layer0, biomeData.getJSONArray("layer0"));
                        arrayToLayer(layer1, biomeData.getJSONArray("layer1"));
                        arrayToLayer(layer2, biomeData.getJSONArray("layer2"));
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
                for(BiomeType b : biomes) biomesArray.put(b.BIOME_ID);
                biomeData.put("biomes", biomesArray);

                biomeData.put("layer0", layerToArray(layer0));
                biomeData.put("layer1", layerToArray(layer1));
                biomeData.put("layer2", layerToArray(layer2));

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

    private JSONArray layerToArray(int[][] layer) {
        JSONArray layerArray = new JSONArray();

        for(int[] l : layer) {
            JSONArray ia = new JSONArray();
            for(int i : l) ia.put(i);
            layerArray.put(ia);
        }

        return layerArray;
    }

    private void arrayToLayer(int[][] layer, JSONArray array) {
        for(int i = 0; i < array.length(); i++) {
            JSONArray idArray = array.getJSONArray(i);

            int[] insert = new int[idArray.length()];

            for(int j = 0; j < idArray.length(); j++) {
                insert[j] = idArray.getInt(j);
            }

            layer[i] = insert;
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
