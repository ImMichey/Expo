package dev.michey.expo.server.packet;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;

public class P11_ChunkData extends Packet {

    public int chunkX;
    public int chunkY;
    public BiomeType[] biomes;
    public DynamicTilePart[][] individualTileData;

}
