package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.util.ExpoShared;

import java.util.ArrayList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.server.main.logic.world.chunk.ServerChunk.*;

public class ServerTile {

    public ServerChunk chunk;
    public int tileX;
    public int tileY;
    public int tileArray;

    public BiomeType biome;
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

    public void updateLayer0(boolean nowHole) {
        var neighbours = getNeighbouringTiles();
        int tis = 0, tid = 0;
        int minTile;

        int n = neighbours[5] == null ? 0 : neighbours[5].layer0[0];
        int e = neighbours[3] == null ? 0 : neighbours[3].layer0[0];
        int s = neighbours[1] == null ? 0 : neighbours[1].layer0[0];
        int w = neighbours[7] == null ? 0 : neighbours[7].layer0[0];

        int ne = neighbours[4] == null ? 0 : neighbours[4].layer0[0];
        int se = neighbours[2] == null ? 0 : neighbours[2].layer0[0];
        int sw = neighbours[0] == null ? 0 : neighbours[0].layer0[0];
        int nw = neighbours[6] == null ? 0 : neighbours[6].layer0[0];

        if(nowHole || (isHoleSoilTile())) {
            minTile = 90;

            if(isHoleSoilTile(n)) tis += NORTH;
            if(isHoleSoilTile(e)) tis += EAST;
            if(isHoleSoilTile(s)) tis += SOUTH;
            if(isHoleSoilTile(w)) tis += WEST;

            if(isHoleSoilTile(ne)) tid += NORTH_EAST;
            if(isHoleSoilTile(se)) tid += SOUTH_EAST;
            if(isHoleSoilTile(sw)) tid += SOUTH_WEST;
            if(isHoleSoilTile(nw)) tid += NORTH_WEST;

            layer0 = chunk.tileIndexToIds(tis, tid, minTile);
        } else {
            layer0 = new int[] {0};
        }
    }

    public void updateLayer1() {
        if(layer1[0] == -1) return;
        boolean grass = isGrassTile();
        boolean sand = isSandTile();
        boolean forest = isForestTile();
        boolean desert = isDesertTile();
        if(!grass && !sand && !forest && !desert) return;

        var neighbours = getNeighbouringTiles();
        var biomes = getNeighbouringBiomes();
        int tis = 0, tid = 0;
        int minTile = 0;

        int n = neighbours[5] == null ? biomes[5].BIOME_LAYER_TEXTURES[1] : neighbours[5].layer1[0];
        int e = neighbours[3] == null ? biomes[3].BIOME_LAYER_TEXTURES[1] : neighbours[3].layer1[0];
        int s = neighbours[1] == null ? biomes[1].BIOME_LAYER_TEXTURES[1] : neighbours[1].layer1[0];
        int w = neighbours[7] == null ? biomes[7].BIOME_LAYER_TEXTURES[1] : neighbours[7].layer1[0];

        int ne = neighbours[4] == null ? biomes[4].BIOME_LAYER_TEXTURES[1] : neighbours[4].layer1[0];
        int se = neighbours[2] == null ? biomes[2].BIOME_LAYER_TEXTURES[1] : neighbours[2].layer1[0];
        int sw = neighbours[0] == null ? biomes[0].BIOME_LAYER_TEXTURES[1] : neighbours[0].layer1[0];
        int nw = neighbours[6] == null ? biomes[6].BIOME_LAYER_TEXTURES[1] : neighbours[6].layer1[0];

        if(grass) {
            minTile = BiomeType.PLAINS.BIOME_LAYER_TEXTURES[1];

            if(isGrassTile(n)) tis += NORTH;
            if(isGrassTile(e)) tis += EAST;
            if(isGrassTile(s)) tis += SOUTH;
            if(isGrassTile(w)) tis += WEST;

            if(isGrassTile(ne)) tid += NORTH_EAST;
            if(isGrassTile(se)) tid += SOUTH_EAST;
            if(isGrassTile(sw)) tid += SOUTH_WEST;
            if(isGrassTile(nw)) tid += NORTH_WEST;
        } else if(sand) {
            minTile = BiomeType.BEACH.BIOME_LAYER_TEXTURES[1];

            if(isSandTile(n)) tis += NORTH;
            if(isSandTile(e)) tis += EAST;
            if(isSandTile(s)) tis += SOUTH;
            if(isSandTile(w)) tis += WEST;

            if(isSandTile(ne)) tid += NORTH_EAST;
            if(isSandTile(se)) tid += SOUTH_EAST;
            if(isSandTile(sw)) tid += SOUTH_WEST;
            if(isSandTile(nw)) tid += NORTH_WEST;
        } else if(forest) {
            minTile = BiomeType.FOREST.BIOME_LAYER_TEXTURES[1];

            if(isForestTile(n)) tis += NORTH;
            if(isForestTile(e)) tis += EAST;
            if(isForestTile(s)) tis += SOUTH;
            if(isForestTile(w)) tis += WEST;

            if(isForestTile(ne)) tid += NORTH_EAST;
            if(isForestTile(se)) tid += SOUTH_EAST;
            if(isForestTile(sw)) tid += SOUTH_WEST;
            if(isForestTile(nw)) tid += NORTH_WEST;
        } else if(desert) {
            minTile = BiomeType.DESERT.BIOME_LAYER_TEXTURES[1];

            if(isDesertTile(n)) tis += NORTH;
            if(isDesertTile(e)) tis += EAST;
            if(isDesertTile(s)) tis += SOUTH;
            if(isDesertTile(w)) tis += WEST;

            if(isDesertTile(ne)) tid += NORTH_EAST;
            if(isDesertTile(se)) tid += SOUTH_EAST;
            if(isDesertTile(sw)) tid += SOUTH_WEST;
            if(isDesertTile(nw)) tid += NORTH_WEST;
        }

        layer1 = chunk.tileIndexToIds(tis, tid, minTile);
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
        if(isGrassTile() || isForestTile()) return 1;
        if(isSandTile() || isDesertTile()) return 2;
        if(isSoilTile()) return 0;
        return -1;
    }

    public static final int BIOME_WILDCARD = -1000;

    /** Layer utility methods below */
    public boolean isSandTile() {
        return isSandTile(layer1[0]);
    }

    public boolean isGrassTile() {
        return isGrassTile(layer1[0]);
    }

    public boolean isSoilTile() {
        return isSoilTile(layer0[0]);
    }

    public boolean isHoleSoilTile() {
        return isHoleSoilTile(layer0[0]);
    }

    public boolean isForestTile() {
        return isForestTile(layer1[0]);
    }

    public boolean isDesertTile() {
        return isDesertTile(layer1[0]);
    }

    public static boolean isSandTile(int id) {
        return id >= 23 && id <= 44;
    }

    public static boolean isGrassTile(int id) {
        return id >= 1 && id <= 22;
    }

    public static boolean isSoilTile(int id) {
        return id == 0;
    }

    public static boolean isHoleSoilTile(int id) {
        return id >= 90 && id <= 111;
    }

    public static boolean isForestTile(int id) {
        return id >= 112 && id <= 133;
    }

    public static boolean isDesertTile(int id) {
        return id >= 134 && id <= 155;
    }

    public static boolean isEmptyTile(int id) {
        return id == -1;
    }

    public static boolean isWaterTile(int id) {
        return (id >= 46 && id <= 89);
    }

    public boolean layerIsEmpty(int layer) {
        int fe = (layer == 0 ? layer0[0] : (layer == 1 ? layer1[0] : layer2[0]));
        return fe == -1;
    }

}
