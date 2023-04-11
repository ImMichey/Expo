package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.noise.BiomeType;

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

    public ServerTile(ServerChunk chunk, int tileX, int tileY, int tileArray) {
        this.chunk = chunk;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileArray = tileArray;
        biome = BiomeType.VOID;
    }

    public void updateLayer1() {
        if(layer1[0] == -1) return;
        boolean grass = isGrassTile();
        boolean sand = isSandTile();
        if(!grass && !sand) return;

        var neighbours = getNeighbouringTiles();
        int tis = 0, tid = 0;
        int minTile = 0;

        int n = neighbours.get(5).layer1[0];
        int e = neighbours.get(3).layer1[0];
        int s = neighbours.get(1).layer1[0];
        int w = neighbours.get(7).layer1[0];

        int ne = neighbours.get(4).layer1[0];
        int se = neighbours.get(2).layer1[0];
        int sw = neighbours.get(0).layer1[0];
        int nw = neighbours.get(6).layer1[0];

        if(grass) {
            minTile = BiomeType.GRASS.BIOME_LAYER_TEXTURES[1];

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
        }

        layer1 = chunk.tileIndexToIds(tis, tid, minTile);
    }

    public List<ServerTile> getNeighbouringTiles() {
        ArrayList<ServerTile> list = new ArrayList<>(8);
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list.add(grid.getTile(tileX - 1, tileY - 1));
        list.add(grid.getTile(tileX, tileY - 1));
        list.add(grid.getTile(tileX + 1, tileY - 1));
        list.add(grid.getTile(tileX + 1, tileY));
        list.add(grid.getTile(tileX + 1, tileY + 1));
        list.add(grid.getTile(tileX, tileY + 1));
        list.add(grid.getTile(tileX - 1, tileY + 1));
        list.add(grid.getTile(tileX - 1, tileY));

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

    public static boolean isSandTile(int id) {
        return id >= 23 && id <= 44;
    }

    public static boolean isGrassTile(int id) {
        return id >= 1 && id <= 22;
    }

    public static boolean isSoilTile(int id) {
        return id == 0;
    }

    public boolean layerIsEmpty(int layer) {
        int fe = (layer == 0 ? layer0[0] : (layer == 1 ? layer1[0] : layer2[0]));
        return fe == -1;
    }

}
