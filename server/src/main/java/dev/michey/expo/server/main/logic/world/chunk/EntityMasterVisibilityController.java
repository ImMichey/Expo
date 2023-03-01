package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;

import java.util.LinkedList;

public class EntityMasterVisibilityController {

    private final ServerDimension dimension;
    private final LinkedList<ServerEntity> evaluationMap;

    public EntityMasterVisibilityController(ServerDimension dimension) {
        this.dimension = dimension;
        evaluationMap = new LinkedList<>();
    }

    public void tick() {
        evaluationMap.clear();
        var m = dimension.getEntityManager();

        // grab all entities that are new or that just moved between chunks
        for(ServerEntity e : m.getAllEntities()) {
            if(!e.trackedVisibility || e.changedChunk) {
                e.trackedVisibility = true;
                evaluationMap.add(e);
            }
        }

        LinkedList<ServerPlayer> playerList = m.getAllPlayers();

        for(ServerPlayer player : playerList) {
            for(ServerEntity toHandle : evaluationMap) {
                if(toHandle.entityId == player.entityId) continue; // don't let the players track themselves
                player.entityVisibilityController.handleEntity(toHandle);
            }
        }

        for(ServerPlayer players : playerList) {
            if(players.changedChunk) {
                players.entityVisibilityController.refreshExistingEntities();
            }
        }
    }

}