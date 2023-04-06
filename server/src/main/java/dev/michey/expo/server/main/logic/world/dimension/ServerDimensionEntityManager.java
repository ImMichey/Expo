package dev.michey.expo.server.main.logic.world.dimension;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;
import dev.michey.expo.util.ExpoShared;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerDimensionEntityManager {

    /** Storage maps */
    private final HashMap<Integer, ServerEntity> idEntityMap;
    private final HashMap<ServerEntityType, LinkedList<ServerEntity>> typeEntityListMap;
    private final ConcurrentLinkedQueue<EntityOperation> entityOperationQueue;

    public ServerDimensionEntityManager() {
        idEntityMap = new HashMap<>();
        typeEntityListMap = new HashMap<>();
        entityOperationQueue = new ConcurrentLinkedQueue<>();

        for(ServerEntityType type : ServerEntityType.values()) {
            typeEntityListMap.put(type, new LinkedList<>());
        }
    }

    /** Main tick method. */
    public void tickEntities(float delta) {
        while(!entityOperationQueue.isEmpty()) {
            EntityOperation op = entityOperationQueue.poll();
            ServerEntity entity = op.payload;
            if(entity == null) entity = getEntityById(op.optionalId);

            if(op.add) {
                addEntityUnsafely(entity);
            } else {
                removeEntityUnsafely(entity);
            }
        }

        for(ServerEntity entity : idEntityMap.values()) {
            entity.tick(delta);

            if(!entity.staticPosition) {
                entity.changedChunk = false;
                int chunkX = ExpoShared.posToChunk(entity.posX);
                int chunkY = ExpoShared.posToChunk(entity.posY);

                if(entity.chunkX != chunkX || entity.chunkY != chunkY) {
                    entity.chunkX = chunkX;
                    entity.chunkY = chunkY;
                    entity.changedChunk = true;
                    entity.onChunkChanged();
                }
            }
        }
    }

    public void addEntitySafely(ServerEntity entity) {
        entityOperationQueue.add(new EntityOperation(entity, true));
    }

    public void removeEntitySafely(ServerEntity entity) {
        entityOperationQueue.add(new EntityOperation(entity, false));
    }

    public void removeEntitySafely(int entityId) {
        entityOperationQueue.add(new EntityOperation(entityId, false));
    }

    /** Adds the ServerEntity to the storage maps without modifying them. */
    private void addEntityUnsafely(ServerEntity entity) {
        idEntityMap.put(entity.entityId, entity);
        typeEntityListMap.get(entity.getEntityType()).add(entity);
        entity.onCreation();
    }

    /** Removes the ServerEntity from the storage maps without modifying them. */
    private void removeEntityUnsafely(ServerEntity entity) {
        idEntityMap.remove(entity.entityId);
        typeEntityListMap.get(entity.getEntityType()).remove(entity);
        entity.onDeletion();
    }

    /** Removes the ServerEntity from the storage maps without modifying them. */
    private void removeEntityUnsafely(int entityId) {
        ServerEntity entity = idEntityMap.get(entityId);
        idEntityMap.remove(entityId);
        typeEntityListMap.get(entity.getEntityType()).remove(entity);
    }

    /** Returns all entities of the same type in the current dimension. */
    public LinkedList<ServerEntity> getEntitiesOf(ServerEntityType type) {
        return typeEntityListMap.get(type);
    }

    /** Returns all player entities in the current dimension. */
    public LinkedList<ServerPlayer> getAllPlayers() {
        LinkedList<ServerEntity> list = getEntitiesOf(ServerEntityType.PLAYER);
        LinkedList<ServerPlayer> copy = new LinkedList<>();

        for(ServerEntity e : list) {
            copy.add((ServerPlayer) e);
        }

        return copy;
    }

    /** Returns the closest player to specified entity. */
    public ServerPlayer getClosestPlayer(ServerEntity to, float maxDistance) {
        float dis = Float.MAX_VALUE;
        ServerPlayer player = null;

        for(ServerPlayer players : getAllPlayers()) {
            float dst = Vector2.dst(players.posX + 5f, players.posY, to.posX, to.posY);

            if(dst <= maxDistance) {
                if(player == null || dis > dst) {
                    dis = dst;
                    player = players;
                }
            }
        }

        return player;
    }

    /** Returns the amount of active entities in the current dimension. */
    public int entityCount() {
        return idEntityMap.size();
    }

    /** Returns a set of all existing entity types in the current dimension. */
    public Set<ServerEntityType> getExistingEntityTypes() {
        return typeEntityListMap.keySet();
    }

    /** Returns the ServerEntity that belongs to the specified entityId. */
    public ServerEntity getEntityById(int entityId) {
        return idEntityMap.get(entityId);
    }

    /** Returns all entities in the current dimension. */
    public Collection<ServerEntity> getAllEntities() {
        return idEntityMap.values();
    }

}
