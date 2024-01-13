package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

import java.util.List;

public class EntityMasterVisibilityController {

    private final ServerDimension dimension;

    public EntityMasterVisibilityController(ServerDimension dimension) {
        this.dimension = dimension;
    }

    public void tick() {
        var m = dimension.getEntityManager();
        List<ServerPlayer> playerList = m.getAllPlayers();

        for(ServerPlayer player : playerList) {
            player.entityVisibilityController.cacheChunkBounds();
        }

        for(ServerPlayer player : playerList) {
            // Handle newly created entities
            for(ServerEntity toHandle : dimension.getEntityManager().getJustAddedEntities()) {
                if(toHandle.entityId == player.entityId) continue;
                player.entityVisibilityController.handleEntity(toHandle);
            }

            // Handle entities (usually living entities) that walked between chunks this tick
            for(ServerEntity toHandle : dimension.getEntityManager().getJustSwitchedChunksEntities()) {
                if(toHandle.entityId == player.entityId) continue;
                player.entityVisibilityController.handleEntity(toHandle);
            }
        }

        for(ServerPlayer players : playerList) {
            if(players.changedChunk || !dimension.getEntityManager().getJustRemovedEntities().isEmpty()) {
                players.entityVisibilityController.refreshExistingEntities();
            }
        }
    }

}