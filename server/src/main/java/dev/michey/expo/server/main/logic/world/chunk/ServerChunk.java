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
    private final int chunkX;
    private final int chunkY;
    private final String chunkKey;
    private final BiomeType[] biomeGrid; // 8x8
    private final int[] tileIndexGrid; // 8x8
    private final boolean[] waterLoggedGrid; // 8x8

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

        biomeGrid = new BiomeType[ROW_TILES * ROW_TILES];
        Arrays.fill(biomeGrid, BiomeType.OCEAN);

        tileIndexGrid = new int[ROW_TILES * ROW_TILES];
        Arrays.fill(tileIndexGrid, 15);

        waterLoggedGrid = new boolean[ROW_TILES * ROW_TILES];
        Arrays.fill(waterLoggedGrid, false);
    }

    public String getChunkKey() {
        return chunkKey;
    }

    public int[] getTileIndexGrid() {
        return tileIndexGrid;
    }

    public boolean[] getWaterLoggedGrid() {
        return waterLoggedGrid;
    }

    private BiomeType biomeAt(int x, int y) {
        return dimension.getChunkHandler().getBiome(x, y);
    }

    public void populate() {
        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        for(int i = 0; i < biomeGrid.length; i++) {
            BiomeType t = biomeGrid[i];
            int index = tileIndexGrid[i];

            if(t == BiomeType.GRASS && index == 15 && MathUtils.random() <= 0.15f) {
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

    public void generate() {
        // log("Generating " + chunkIdentifier());

        int wx = ExpoShared.chunkToPos(chunkX);
        int wy = ExpoShared.chunkToPos(chunkY);

        int btx = ExpoShared.posToTile(wx);
        int bty = ExpoShared.posToTile(wy);

        for(int i = 0; i < biomeGrid.length; i++) {
            int x = btx + i % 8;
            int y = bty + i / 8;
            biomeGrid[i] = dimension.getChunkHandler().getBiome(x, y);

            int tileIndex = 0;

            BiomeType n = biomeAt(x, y + 1);
            BiomeType e = biomeAt(x + 1, y);
            BiomeType s = biomeAt(x, y - 1);
            BiomeType w = biomeAt(x - 1, y);

            BiomeType ne = biomeAt(x + 1, y + 1);
            BiomeType se = biomeAt(x + 1, y - 1);
            BiomeType sw = biomeAt(x - 1, y - 1);
            BiomeType nw = biomeAt(x - 1, y + 1);

            if(!BiomeType.isWater(biomeGrid[i]) &&
                    (BiomeType.isWater(n) || BiomeType.isWater(e) || BiomeType.isWater(s) || BiomeType.isWater(w) ||
            BiomeType.isWater(ne) || BiomeType.isWater(se) || BiomeType.isWater(sw) || BiomeType.isWater(nw))) {
                waterLoggedGrid[i] = true;
            }

            if(n == biomeGrid[i]) tileIndex += 1;
            if(e == biomeGrid[i]) tileIndex += 2;
            if(s == biomeGrid[i]) tileIndex += 4;
            if(w == biomeGrid[i]) tileIndex += 8;

            if(tileIndex == 15) {
                // Check corners
                int subTileIndex = 0;

                if(biomeAt(x + 1, y + 1) == biomeGrid[i]) subTileIndex += 1;
                if(biomeAt(x + 1, y - 1) == biomeGrid[i]) subTileIndex += 2;
                if(biomeAt(x - 1, y - 1) == biomeGrid[i]) subTileIndex += 4;
                if(biomeAt(x - 1, y + 1) == biomeGrid[i]) subTileIndex += 8;

                if(subTileIndex < 15) {
                    tileIndex += subTileIndex;
                }
            } else if(tileIndex == 7) {
                // Check corners
                int subTileIndex = 0;

                if(biomeAt(x + 1, y + 1) == biomeGrid[i]) subTileIndex += 1;
                if(biomeAt(x + 1, y - 1) == biomeGrid[i]) subTileIndex += 2;

                if(subTileIndex != 3) {
                    tileIndex += 25 + subTileIndex;
                }
            } else if(tileIndex == 11) {
                // Check corners
                int subTileIndex = 0;

                if(biomeAt(x + 1, y + 1) == biomeGrid[i]) subTileIndex += 1;
                if(biomeAt(x - 1, y + 1) == biomeGrid[i]) subTileIndex += 2;

                if(subTileIndex != 3) {
                    tileIndex += 24 + subTileIndex;
                }
            } else if(tileIndex == 13) {
                // Check corners
                int subTileIndex = 0;

                if(biomeAt(x - 1, y + 1) == biomeGrid[i]) subTileIndex += 1;
                if(biomeAt(x - 1, y - 1) == biomeGrid[i]) subTileIndex += 2;

                if(subTileIndex != 3) {
                    tileIndex += 25 + subTileIndex;
                }
            } else if(tileIndex == 14) {
                // Check corners
                int subTileIndex = 0;

                if(biomeAt(x - 1, y - 1) == biomeGrid[i]) subTileIndex += 1;
                if(biomeAt(x + 1, y - 1) == biomeGrid[i]) subTileIndex += 2;

                if(subTileIndex != 3) {
                    tileIndex += 27 + subTileIndex;
                }
            } else if(tileIndex == 3) {
                // Check corner
                if(biomeAt(x + 1, y + 1) != biomeGrid[i]) tileIndex = 44;
            } else if(tileIndex == 6) {
                // Check corner
                if(biomeAt(x + 1, y - 1) != biomeGrid[i]) tileIndex = 45;
            } else if(tileIndex == 9) {
                // Check corner
                if(biomeAt(x - 1, y + 1) != biomeGrid[i]) tileIndex = 46;
            } else if(tileIndex == 12) {
                // Check corner
                if(biomeAt(x - 1, y - 1) != biomeGrid[i]) tileIndex = 47;
            }

            tileIndexGrid[i] = tileIndex;
        }

        populate();
    }

    public BiomeType[] getBiomeGrid() {
        return biomeGrid;
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
            generate();
        } else {
            try {
                JSONObject object = new JSONObject(loadedString);

                {
                    // biomeData
                    JSONObject biomeData = object.getJSONObject("biomeData");

                        // biomeTypeGrid
                        JSONArray biomeTypeGrid = biomeData.getJSONArray("biomeTypeGrid");
                        for(int i = 0; i < biomeGrid.length; i++) biomeGrid[i] = BiomeType.idToBiome(biomeTypeGrid.getInt(i));

                        // tileIndexGrid
                        JSONArray tig = biomeData.getJSONArray("tileIndexGrid");
                        for(int i = 0; i < tileIndexGrid.length; i++) tileIndexGrid[i] = tig.getInt(i);

                        // waterLoggedGrid
                        JSONArray wlg = biomeData.getJSONArray("waterLoggedGrid");
                        for(int i = 0; i < waterLoggedGrid.length; i++) waterLoggedGrid[i] = wlg.getBoolean(i);
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
                generate();
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

                // biomeTypeGrid
                JSONArray biomeTypeGrid = new JSONArray();
                for(BiomeType bt : biomeGrid) biomeTypeGrid.put(bt.BIOME_ID);
                biomeData.put("biomeTypeGrid", biomeTypeGrid);

                // tileIndexGrid
                JSONArray tig = new JSONArray();
                for(int i : tileIndexGrid) tig.put(i);
                biomeData.put("tileIndexGrid", tig);

                // waterLoggedGrid
                JSONArray wlg = new JSONArray();
                for(boolean b : waterLoggedGrid) wlg.put(b);
                biomeData.put("waterLoggedGrid", wlg);

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

    private String chunkIdentifier() {
        return "ServerChunk [" + chunkX + "," + chunkY + "]";
    }

    private File getChunkFile() {
        String path = ExpoServerBase.get().getWorldSaveHandler().getPathDimensionSpecificFolder(dimension.getDimensionName());
        return new File(path + File.separator + chunkX + "," + chunkY);
    }

}
