package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.util.ExpoShared;

import java.util.Arrays;

public class ServerTile {

    public ServerChunk chunk;
    public int tileX;
    public int tileY;
    public int tileArray;

    public BiomeType biome;
    public TileLayerType[] layerTypes;
    public int[] layer0;
    public int[] layer1;
    public int[] layer2;

    public float digHealth;
    public long digTimestamp;

    public ServerTile(ServerChunk chunk, int tileX, int tileY, int tileArray) {
        this.chunk = chunk;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileArray = tileArray;
        biome = BiomeType.VOID;
    }

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    public static final int NORTH_EAST = 1;
    public static final int SOUTH_EAST = 2;
    public static final int SOUTH_WEST = 4;
    public static final int NORTH_WEST = 8;

    /** Updates the first layer (0). If TileLayerType is null, the biome is used. */
    public void updateLayer0(TileLayerType type) {
        if(type == null) {
            layerTypes[0] = TileLayerType.biomeToLayer0(biome);
            int minTile = biome.BIOME_LAYER_TEXTURES[0];

            if(biome == BiomeType.OCEAN_DEEP) {
                layer0 = new int[] {minTile};
            } else {
                layer0 = runTextureGrab(minTile, 0);
            }
        } else {
            layerTypes[0] = type;
            int[] minTile = TileLayerType.typeToTextures(type);

            if(minTile == null) {
                layer0 = new int[] {-1};
            } else {
                if(minTile.length < 2) {
                    layer0 = new int[] {minTile[0]};
                } else {
                    layer0 = runTextureGrab(minTile[0], 0);
                }
            }
        }
    }

    /** This method is called when an adjacent layer 0 tile has been updated and this tile potentially needs to adjust its texture. */
    public boolean updateLayer0Adjacent() {
        int[] textures = TileLayerType.typeToTextures(layerTypes[0]);
        if(textures == null) return false;
        if(textures.length < 2) return false;
        int[] old = layer0;
        layer0 = runTextureGrab(textures[0], 0);
        return !Arrays.equals(old, layer0);
    }

    /** This method is called when an adjacent layer 1 tile has been updated and this tile potentially needs to adjust its texture.
     * Returns whether an update packet is needed or not. */
    public boolean updateLayer1Adjacent() {
        int[] textures = TileLayerType.typeToTextures(layerTypes[1]);
        if(textures == null) return false;
        if(textures.length < 2) return false;
        int[] old = layer1;
        layer1 = runTextureGrab(textures[0], 1);
        return !Arrays.equals(old, layer1);
    }

    /** This method is called when an adjacent layer 2 tile has been updated and this tile potentially needs to adjust its texture. */
    public void updateLayer2Adjacent() {

    }

    private int[] runTextureGrab(int minTile, int layer) {
        int[] indices = indexStraightDiagonal(layer, tileX, tileY);
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

    /** Updates the second layer (1). If TileLayerType is null, the biome is used. */
    public void updateLayer1(TileLayerType type) {
        if(type == null) {
            layerTypes[1] = TileLayerType.biomeToLayer1(biome);

            int minTile = biome.BIOME_LAYER_TEXTURES[1];
            layer1 = runTextureGrab(minTile, 1);
        } else {
            layerTypes[1] = type;
            int[] minTile = TileLayerType.typeToTextures(type);

            if(minTile == null) {
                layer1 = new int[] {-1};
            } else {
                if(minTile.length < 2) {
                    layer1 = new int[] {minTile[0]};
                } else {
                    layer1 = runTextureGrab(minTile[0], 1);
                }
            }
        }
    }

    public void updateLayer2(TileLayerType type) {
        layerTypes[2] = TileLayerType.EMPTY;
        layer2 = new int[] {-1};
    }

    private int[] indexStraightDiagonal(int checkLayer, int x, int y) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        TileLayerType b = layerTypes[checkLayer];
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

    public boolean dig(float hp) {
        long now = System.currentTimeMillis();
        long diff = now - digTimestamp;

        if(diff >= ExpoShared.RESET_TILE_DIG_HEALTH_AFTER_MILLIS) {
            digHealth = 20.0f - hp;
            digTimestamp = now;
            return false;
        }

        digHealth -= hp;
        digTimestamp = now;
        return digHealth <= 0;
    }

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
                "chunk=" + chunk +
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

    public boolean isType(TileLayerType type, int layer) {
        return layerTypes[layer] == type;
    }

}
