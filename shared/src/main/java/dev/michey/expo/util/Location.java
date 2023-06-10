package dev.michey.expo.util;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class Location {

    public String dimension;
    public float x;
    public float y;

    public int chunkX;
    public int chunkY;
    public int tileX;
    public int tileY;
    public int tileArrayPos;

    public Location(String dimension, float x, float y) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
    }

    public Location() {
        this.dimension = ExpoShared.DIMENSION_OVERWORLD;
        this.x = 0f;
        this.y = 0f;
    }

    public Location toDetailedLocation() {
        tileX = ExpoShared.posToTile(x);
        tileY = ExpoShared.posToTile(y);

        chunkX = ExpoShared.posToChunk(x);
        chunkY = ExpoShared.posToChunk(y);

        int startTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(chunkX));
        int startTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(chunkY));
        int mouseRelativeTileX = tileX - startTileX;
        int mouseRelativeTileY = tileY - startTileY;

        tileArrayPos = mouseRelativeTileY * ROW_TILES + mouseRelativeTileX;
        return this;
    }

    @Override
    public String toString() {
        return "Location{" +
                "dimension='" + dimension + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", chunkX=" + chunkX +
                ", chunkY=" + chunkY +
                ", tileX=" + tileX +
                ", tileY=" + tileY +
                ", tileArrayPos=" + tileArrayPos +
                '}';
    }

}
