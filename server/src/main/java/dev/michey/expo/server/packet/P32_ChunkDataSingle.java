package dev.michey.expo.server.packet;

public class P32_ChunkDataSingle extends Packet {

    public int chunkX;
    public int chunkY;
    public int layer;
    public int tileArray;
    public int[] data;

}