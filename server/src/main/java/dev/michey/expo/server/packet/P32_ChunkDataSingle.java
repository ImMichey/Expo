package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;

public class P32_ChunkDataSingle extends Packet {

    public int chunkX;
    public int chunkY;
    public int layer;
    public int tileArray;
    public DynamicTilePart tile;
    public float grassColor;

}