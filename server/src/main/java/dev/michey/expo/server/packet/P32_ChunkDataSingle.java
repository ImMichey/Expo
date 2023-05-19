package dev.michey.expo.server.packet;

import dev.michey.expo.noise.TileLayerType;

public class P32_ChunkDataSingle extends Packet {

    public int chunkX;
    public int chunkY;
    public int layer;
    public int tileArray;
    public int[] data;
    public TileLayerType[] layerTypes;

}