package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;

public class P50_TileFullUpdate extends Packet {

    public int chunkX;
    public int chunkY;
    public int tileArray;
    public DynamicTilePart[] dynamicTileParts;

}
