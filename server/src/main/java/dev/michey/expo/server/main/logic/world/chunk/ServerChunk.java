package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
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

    private BiomeType biomeAt(int x, int y) {
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
                            generatedEntity.onGeneration(false);

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
                                            spreadEntity.onGeneration(true);

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

        for(ServerTile st : tiles) {
            st.layer0 = new int[] {-1};
            st.layer1 = new int[] {-1};
            st.layer2 = new int[] {-1};
        }

        for(int i = 0; i < tiles.length; i++) {
            ServerTile tile = tiles[i];
            int x = btx + i % 8;
            int y = bty + i / 8;

            // Assign biome.
            tile.biome = dimension.getChunkHandler().getBiome(x, y);
            BiomeType b = tile.biome;

            int[] tileBounds = b.BIOME_LAYER_TEXTURES;

            if(tileBounds != null) {
                int minTile = tileBounds[1];

                // LAYER 0
                int layer0Id = tileBounds[0];
                tile.layer0 = new int[] {layer0Id};

                // LAYER 1
                int[] indices = indexStraightDiagonal(b.BIOME_NEIGHBOURS, x, y);
                int tis = indices[0];
                int tid = indices[1];

                if(tis == 0 && tid == 0) {
                    // Special case, no neighbour
                    tile.layer1 = new int[] {minTile + 1};
                } else if(tis == 15 && tid == 15) {
                    // Special case, every 8 neighbours are same tile
                    tile.layer1 = new int[] {minTile};
                } else if(tis == 15) {
                    // N E S W all valid neighbours (straight)
                    if(tid == 0) {
                        // No diagonal neighbours
                        tile.layer1 = new int[] {minTile + 20, minTile + 21, minTile + 18, minTile + 19};
                    } else {
                        // tig between [1-14]
                        boolean northWest = tid / NORTH_WEST == 1;
                        boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
                        boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
                        boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

                        tile.layer1 = new int[] {
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

                    tile.layer1 = new int[] {c1, c2, c3, c4};
                }
            }
        }

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
            }
    };

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    public static final int NORTH_EAST = 1;
    public static final int SOUTH_EAST = 2;
    public static final int SOUTH_WEST = 4;
    public static final int NORTH_WEST = 8;

    private int[] indexStraightDiagonal(String[] acceptedNeighbours, int x, int y) {
        BiomeType[] biomes = new BiomeType[acceptedNeighbours.length];
        for(int i = 0; i < acceptedNeighbours.length; i++) {
            biomes[i] = BiomeType.valueOf(acceptedNeighbours[i]);
        }
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

    public int[] tileIndexToIds(int tis, int tid, int minTile) {
        int[] ids;

        if(tis == 0 && tid == 0) {
            // Special case, no neighbour
            ids = new int[] {minTile + 1};
        } else if(tis == 15 && tid == 15) {
            // Special case, every 8 neighbours are same tile
            ids = new int[] {minTile};
        } else if(tis == 15) {
            // N E S W all valid neighbours (straight)
            if(tid == 0) {
                // No diagonal neighbours
                ids = new int[] {minTile + 20, minTile + 21, minTile + 18, minTile + 19};
            } else {
                // tig between [1-14]
                boolean northWest = tid / NORTH_WEST == 1;
                boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
                boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
                boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

                ids = new int[] {
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
                if (west && south && southWest) {
                    c1 = minTile + 8;
                } else if (west && south) {
                    c1 = minTile + 20;
                } else if (west && southWest) {
                    c1 = minTile + 16;
                } else if (south && southWest) {
                    c1 = minTile + 4;
                } else if (west) {
                    c1 = minTile + 16;
                } else if (south) {
                    c1 = minTile + 4;
                } else {
                    c1 = minTile + 12;
                }
            }

            { // Corner Bottom Right
                if (east && south && southEast) {
                    c2 = minTile + 5;
                } else if (east && south) {
                    c2 = minTile + 21;
                } else if (east && southEast) {
                    c2 = minTile + 13;
                } else if (south && southEast) {
                    c2 = minTile + 9;
                } else if (east) {
                    c2 = minTile + 13;
                } else if (south) {
                    c2 = minTile + 9;
                } else {
                    c2 = minTile + 17;
                }
            }

            { // Corner Top Left
                if (west && north && northWest) {
                    c3 = minTile + 14;
                } else if (west && north) {
                    c3 = minTile + 18;
                } else if (west && northWest) {
                    c3 = minTile + 6;
                } else if (north && northWest) {
                    c3 = minTile + 10;
                } else if (west) {
                    c3 = minTile + 6;
                } else if (north) {
                    c3 = minTile + 10;
                } else {
                    c3 = minTile + 2;
                }
            }

            { // Corner Top Right
                if (east && north && northEast) {
                    c4 = minTile + 11;
                } else if (east && north) {
                    c4 = minTile + 19;
                } else if (east && northEast) {
                    c4 = minTile + 3;
                } else if (north && northEast) {
                    c4 = minTile + 15;
                } else if (east) {
                    c4 = minTile + 3;
                } else if (north) {
                    c4 = minTile + 15;
                } else {
                    c4 = minTile + 7;
                }
            }

            ids = new int[] {c1, c2, c3, c4};
        }

        return ids;
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
