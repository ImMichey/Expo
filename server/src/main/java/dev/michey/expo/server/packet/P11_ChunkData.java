package dev.michey.expo.server.packet;

import dev.michey.expo.noise.BiomeType;

public class P11_ChunkData extends Packet {

    public int chunkX;
    public int chunkY;
    public BiomeType[] biomes;
    public int[][] layer0;
    public int[][] layer1;
    public int[][] layer2;

}
