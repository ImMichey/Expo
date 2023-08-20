package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.Arrays;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class ServerTile {

    public ServerChunk chunk;
    public int tileX; // Absolute tile X
    public int tileY; // Absolute tile Y
    public int tileArray;

    public BiomeType biome;
    public DynamicTilePart[] dynamicTileParts;

    public float foliageColor;
    public float[] ambientOcclusion;

    public ServerTile(ServerChunk chunk, int tileX, int tileY, int tileArray) {
        this.chunk = chunk;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileArray = tileArray;
        biome = BiomeType.VOID;
        ambientOcclusion = new float[4];
        generateBaseBlendingAO();
    }

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    public static final int NORTH_EAST = 1;
    public static final int SOUTH_EAST = 2;
    public static final int SOUTH_WEST = 4;
    public static final int NORTH_WEST = 8;

    public void updateLayer(int layer, TileLayerType type) {
        if(layer == 0) {
            updateLayer0(type);
        } else if(layer == 1) {
            updateLayer1(type);
        } else {
            updateLayer2(type);
        }
    }

    public void updateLayer0(TileLayerType type) {
        TileLayerType use = type == null ? TileLayerType.biomeToLayer0(biome) : type;
        dynamicTileParts[0].update(use);
        int[] td = dynamicTileParts[0].emulatingType.TILE_ID_DATA;

        if(td.length == 1) {
            dynamicTileParts[0].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[0].setTileIds(runTextureGrab(td[0], 0));
        }

        /*
        for(var x : chunk.getDimension().getChunkHandler().getGenSettings().getNoiseSettings().postProcessList.values()) {
            x.postProcessorLogic.getLayerType()
        }
        */
    }

    public void generateBaseBlendingAO() {
        foliageColor = chunk.getDimension().getChunkHandler().getElevationTemperatureMoisture(tileX, tileY)[1];
        Arrays.fill(ambientOcclusion, 0.0f);
    }

    public void generateAO() {
        if(hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE)) {
            Arrays.fill(ambientOcclusion, 1.0f);
            ExpoLogger.log(tileX + "," + tileY + ": " + Arrays.toString(ambientOcclusion) + " SP");
            return;
        }

        ServerTile[] neighbours = getNeighbouringTiles();
        boolean n = neighbours[5] != null && neighbours[5].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean e = neighbours[3] != null && neighbours[3].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean s = neighbours[1] != null && neighbours[1].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean w = neighbours[7] != null && neighbours[7].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean ne = neighbours[4] != null && neighbours[4].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean se = neighbours[2] != null && neighbours[2].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean sw = neighbours[0] != null && neighbours[0].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);
        boolean nw = neighbours[6] != null && neighbours[6].hasTileBasedEntityB(ServerEntityType.DYNAMIC_3D_TILE);

        // [0] = Bottom Left
        if(s || w || sw) {
            ambientOcclusion[0] = 1.0f;
        } else {
            ambientOcclusion[0] = 0.0f;
        }

        // [1] = Top Left
        if(n || w || nw) {
            ambientOcclusion[1] = 1.0f;
        } else {
            ambientOcclusion[1] = 0.0f;
        }

        // [2] = Top Right
        if(n || e || ne) {
            ambientOcclusion[2] = 1.0f;
        } else {
            ambientOcclusion[2] = 0.0f;
        }

        // [3] = Bottom Right
        if(s || e || se) {
            ambientOcclusion[3] = 1.0f;
        } else {
            ambientOcclusion[3] = 0.0f;
        }

        ExpoLogger.log(tileX + "," + tileY + ": " + Arrays.toString(ambientOcclusion));
    }

    public void updateLayer1(TileLayerType type) {
        TileLayerType use = type == null ? TileLayerType.biomeToLayer1(biome) : type;
        dynamicTileParts[1].update(use);
        int[] td = dynamicTileParts[1].emulatingType.TILE_ID_DATA;

        if(td.length == 1) {
            dynamicTileParts[1].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[1].setTileIds(runTextureGrab(td[0], 1));
        }

        if(use == TileLayerType.ROCK || use == TileLayerType.DIRT) {
            int x = tileArray % ROW_TILES;
            int y = tileArray / ROW_TILES;

            ServerDynamic3DTile entity = new ServerDynamic3DTile();
            entity.posX = ExpoShared.tileToPos(tileX);
            entity.posY = ExpoShared.tileToPos(tileY);
            entity.setStaticEntity();
            entity.layerIds = dynamicTileParts[1].layerIds;
            entity.emulatingType = use;

            ServerWorld.get().registerServerEntity(chunk.getDimension().getDimensionName(), entity);
            entity.attachToTile(chunk, x, y);

            dynamicTileParts[1].setTileIds(new int[] {-1});
        }
    }

    public void updateLayer2(TileLayerType type) {
        TileLayerType use = type == null ? TileLayerType.biomeToLayer2(biome) : type;
        dynamicTileParts[2].update(use);
        int[] td = dynamicTileParts[2].emulatingType.TILE_ID_DATA;

        if(td.length == 1) {
            dynamicTileParts[2].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[2].setTileIds(runTextureGrab(td[0], 2));
        }
    }

    public boolean updateLayerAdjacent(int layer) {
        if(layer == 0) {
            return updateLayer0Adjacent();
        } else if(layer == 1) {
            return updateLayer1Adjacent();
        }

        return updateLayer2Adjacent();
    }

    /** This method is called when an adjacent layer 0 tile has been updated and this tile potentially needs to adjust its texture. */
    public boolean updateLayer0Adjacent() {
        int[] td = dynamicTileParts[0].emulatingType.TILE_ID_DATA;
        int[] old = dynamicTileParts[0].layerIds;

        if(td.length == 1) {
            dynamicTileParts[0].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[0].setTileIds(runTextureGrab(td[0], 0));
        }

        return !Arrays.equals(old, dynamicTileParts[0].layerIds);
    }

    /** This method is called when an adjacent layer 1 tile has been updated and this tile potentially needs to adjust its texture.
     * Returns whether an update packet is needed or not. */
    public boolean updateLayer1Adjacent() {
        int[] td = dynamicTileParts[1].emulatingType.TILE_ID_DATA;
        int[] old = dynamicTileParts[1].layerIds;

        if(dynamicTileParts[1].emulatingType != TileLayerType.ROCK && dynamicTileParts[1].emulatingType != TileLayerType.DIRT) {
            if(td.length == 1) {
                dynamicTileParts[1].setTileIds(new int[] {td[0]});
            } else {
                dynamicTileParts[1].setTileIds(runTextureGrab(td[0], 1));
            }
        }

        return !Arrays.equals(old, dynamicTileParts[1].layerIds);
    }

    /** This method is called when an adjacent layer 2 tile has been updated and this tile potentially needs to adjust its texture. */
    public boolean updateLayer2Adjacent() {
        return false;
    }

    public static int[] runTextureGrab(int minTile, int[] indices) {
        int tis = indices[0];
        int tid = indices[1];

        if(tis == 0 && tid == 0) {
            // Special case, no neighbour
            return new int[] {minTile + 1};
        } else if(tis == 15 && tid == 15) {
            // Special case, every 8 neighbours are same tile
            return new int[] {minTile};
        } else if(tis == 15) {
            // N E S W all valid neighbours (straight)
            if(tid == 0) {
                // No diagonal neighbours
                return new int[] {minTile + 20, minTile + 21, minTile + 18, minTile + 19};
            } else {
                // tig between [1-14]
                boolean northWest = tid / NORTH_WEST == 1;
                boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
                boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
                boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

                return new int[] {
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

            return new int[] {c1, c2, c3, c4};
        }
    }

    private int[] runTextureGrab(int minTile, int layer, TileLayerType type) {
        return runTextureGrab(minTile, indexStraightDiagonal(layer, tileX, tileY, type));
    }

    public int[] runTextureGrab(int minTile, int layer) {
        return runTextureGrab(minTile, layer, dynamicTileParts[layer].emulatingType);
    }

    private int[] indexStraightDiagonal(int checkLayer, int x, int y, TileLayerType b) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        int tis = 0, tid = 0;

        if(TileLayerType.isConnected(n, b)) tis += NORTH;
        if(TileLayerType.isConnected(e, b)) tis += EAST;
        if(TileLayerType.isConnected(s, b)) tis += SOUTH;
        if(TileLayerType.isConnected(w, b)) tis += WEST;
        if(TileLayerType.isConnected(ne, b)) tid += NORTH_EAST;
        if(TileLayerType.isConnected(se, b)) tid += SOUTH_EAST;
        if(TileLayerType.isConnected(sw, b)) tid += SOUTH_WEST;
        if(TileLayerType.isConnected(nw, b)) tid += NORTH_WEST;

        return new int[] {tis, tid};
    }

    private Pair<int[], TileLayerType[]> indexStraightDiagonalPrecise(int checkLayer, int x, int y, TileLayerType b) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        int tis = 0, tid = 0;

        if(n == b) tis += NORTH;
        if(e == b) tis += EAST;
        if(s == b) tis += SOUTH;
        if(w == b) tis += WEST;
        if(ne == b) tid += NORTH_EAST;
        if(se == b) tid += SOUTH_EAST;
        if(sw == b) tid += SOUTH_WEST;
        if(nw == b) tid += NORTH_WEST;

        return new Pair<>(new int[] {tis, tid}, new TileLayerType[] {n, e, s, w, ne, se, sw, nw});
    }

    private int[] indexStraightDiagonalX(int checkLayer, int x, int y, TileLayerType b) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        int tis = 0, tid = 0;

        if(n == b) tis += NORTH;
        if(e == b) tis += EAST;
        if(s == b) tis += SOUTH;
        if(w == b) tis += WEST;
        if(ne == b) tid += NORTH_EAST;
        if(se == b) tid += SOUTH_EAST;
        if(sw == b) tid += SOUTH_WEST;
        if(nw == b) tid += NORTH_WEST;

        return new int[] {tis, tid};
    }

    public boolean dig(int layer, float damage) {
        return dynamicTileParts[layer].hit(damage);
    }

    public void playTileSound(String sound) {
        if(sound != null) {
            ServerPackets.p24PositionalSound(sound,
                    ExpoShared.tileToPos(tileX) + 8f, ExpoShared.tileToPos(tileY) + 8f,
                    ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(chunk.getDimension(), chunk.chunkX, chunk.chunkY));
        }
    }

    // Bottom Left -> Bottom Right -> Top Right -> Top Left -> Bottom Left
    public ServerTile[] getNeighbouringTiles() {
        ServerTile[] list = new ServerTile[8];
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list[0] = grid.getTile(tileX - 1, tileY - 1);
        list[1] = grid.getTile(tileX, tileY - 1);
        list[2] = grid.getTile(tileX + 1, tileY - 1);
        list[3] = grid.getTile(tileX + 1, tileY);
        list[4] = grid.getTile(tileX + 1, tileY + 1);
        list[5] = grid.getTile(tileX, tileY + 1);
        list[6] = grid.getTile(tileX - 1, tileY + 1);
        list[7] = grid.getTile(tileX - 1, tileY);

        return list;
    }

    // North -> East -> South -> West (4 tiles)
    public ServerTile[] getNeighbouringTilesNESW() {
        ServerTile[] list = new ServerTile[4];
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list[0] = grid.getTile(tileX, tileY + 1);
        list[1] = grid.getTile(tileX + 1, tileY);
        list[2] = grid.getTile(tileX, tileY - 1);
        list[3] = grid.getTile(tileX - 1, tileY);

        return list;
    }

    public BiomeType[] getNeighbouringBiomes() {
        BiomeType[] list = new BiomeType[8];
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list[0] = grid.getBiome(tileX - 1, tileY - 1);
        list[1] = grid.getBiome(tileX, tileY - 1);
        list[2] = grid.getBiome(tileX + 1, tileY - 1);
        list[3] = grid.getBiome(tileX + 1, tileY);
        list[4] = grid.getBiome(tileX + 1, tileY + 1);
        list[5] = grid.getBiome(tileX, tileY + 1);
        list[6] = grid.getBiome(tileX - 1, tileY + 1);
        list[7] = grid.getBiome(tileX - 1, tileY);

        return list;
    }

    @Override
    public String toString() {
        return "ServerTile{" +
                "chunkX=" + chunk.chunkX +
                ", chunkY=" + chunk.chunkY +
                ", tileX=" + tileX +
                ", tileY=" + tileY +
                ", tileArray=" + tileArray +
                ", biome=" + biome +
                '}';
    }

    public int toParticleColorId() {
        if(isType(TileLayerType.GRASS, 1) || isType(TileLayerType.FOREST, 1)) return 1;
        if(isType(TileLayerType.SAND, 1) || isType(TileLayerType.DESERT, 1)) return 2;
        if(isType(TileLayerType.SOIL, 0)) return 0;
        return -1;
    }

    public ServerEntity hasTileBasedEntity(ServerEntityType type) {
        if(!chunk.hasTileBasedEntities()) return null;
        int entityId = chunk.getTileBasedEntityIdGrid()[tileArray];
        if(entityId == -1) return null;
        ServerEntity found = ServerWorld.get().getDimension(chunk.getDimension().getDimensionName()).getEntityManager().getEntityById(entityId);
        if(found == null) return null;
        if(found.getEntityType() == type) return found;
        return null;
    }

    public boolean hasTileBasedEntityB(ServerEntityType... type) {
        if(!chunk.hasTileBasedEntities()) return false;
        int entityId = chunk.getTileBasedEntityIdGrid()[tileArray];
        if(entityId == -1) return false;
        ServerEntity found = ServerWorld.get().getDimension(chunk.getDimension().getDimensionName()).getEntityManager().getEntityById(entityId);
        if(found == null) return false;
        for(ServerEntityType t : type) {
            if(found.getEntityType() == t) return true;
        }
        return false;
    }

    public boolean isType(TileLayerType type, int layer) {
        return dynamicTileParts[layer].emulatingType == type;
    }

}
