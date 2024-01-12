package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public record PlayerChunkMemory(ServerPlayer player, int originChunkX, int originChunkY, ServerChunk[] chunkArray) {

}
