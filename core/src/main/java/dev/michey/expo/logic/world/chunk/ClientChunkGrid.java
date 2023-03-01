package dev.michey.expo.logic.world.chunk;

import dev.michey.expo.noise.BiomeType;

import java.util.concurrent.ConcurrentHashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientChunkGrid {

    private static ClientChunkGrid INSTANCE;

    private ConcurrentHashMap<String, ClientChunk> clientChunkMap;

    public ClientChunkGrid() {
        clientChunkMap = new ConcurrentHashMap<>();
        INSTANCE = this;
    }

    public void updateChunkData(int chunkX, int chunkY, BiomeType[] biomeData, int[] tileIndexData, boolean[] waterLoggedData) {
        //log("upodateChunkData " + chunkX + " " + chunkY);
        String key = chunkX + "," + chunkY;
        clientChunkMap.put(key, new ClientChunk(chunkX, chunkY, biomeData, tileIndexData, waterLoggedData));
    }

    public ClientChunk getChunk(int x, int y) {
        return clientChunkMap.get(x + "," + y);
    }

    public static ClientChunkGrid get() {
        return INSTANCE;
    }

}
