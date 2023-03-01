package dev.michey.expo.server.packet;

import dev.michey.expo.noise.BiomeType;

public class P11_ChunkData extends Packet {

    public int chunkX;
    public int chunkY;
    public BiomeType[] biomeData; // length = 64
    public int[] tileIndexData; // length = 64
    public boolean[] waterLoggedData;

    public P11_ChunkData(int chunkX, int chunkY, BiomeType[] biomeData, int[] tileIndexData, boolean[] waterLoggedData) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.biomeData = biomeData;
        this.tileIndexData = tileIndexData;
        this.waterLoggedData = waterLoggedData;
    }

    public P11_ChunkData() {

    }

}
