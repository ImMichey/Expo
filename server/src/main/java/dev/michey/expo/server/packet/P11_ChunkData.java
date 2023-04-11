package dev.michey.expo.server.packet;

import dev.michey.expo.server.main.logic.world.chunk.ServerTile;

public class P11_ChunkData extends Packet {

    public int chunkX;
    public int chunkY;
    public ServerTile[] tiles;

}
