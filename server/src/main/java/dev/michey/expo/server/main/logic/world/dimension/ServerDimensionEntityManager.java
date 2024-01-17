package dev.michey.expo.server.main.logic.world.dimension;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.DamageableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerDimensionEntityManager {

    /** Parent */
    private final ServerDimension dimension;

    /** Storage maps */
    private final TreeMap<Integer, ServerEntity> idEntityMap;
    private final HashMap<ServerEntityType, ArrayList<ServerEntity>> typeEntityListMap;
    private final ConcurrentLinkedQueue<EntityOperation> entityOperationQueue;
    private final HashMap<Integer, ServerEntity> damageableEntityMap;
    private final HashMap<Integer, List<ServerItem>> mergeItemMap;
    private final ArrayList<ServerEntity> justAddedEntityList;
    private final ArrayList<ServerEntity> justRemovedEntityList;
    private final ArrayList<ServerEntity> justSwitchedChunksEntityList;

    public ServerDimensionEntityManager(ServerDimension dimension) {
        this.dimension = dimension;
        idEntityMap = new TreeMap<>();
        typeEntityListMap = new HashMap<>();
        damageableEntityMap = new HashMap<>();
        entityOperationQueue = new ConcurrentLinkedQueue<>();
        mergeItemMap = new HashMap<>();
        justAddedEntityList = new ArrayList<>(4096);
        justRemovedEntityList = new ArrayList<>(4096);
        justSwitchedChunksEntityList = new ArrayList<>(256);

        for(ServerEntityType type : ServerEntityType.values()) {
            typeEntityListMap.put(type, new ArrayList<>());
        }
    }

    /** Main tick method. */
    public void tickEntities(float delta) {
        justAddedEntityList.clear();
        justRemovedEntityList.clear();
        justSwitchedChunksEntityList.clear();

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

        for(ServerEntityType type : getExistingEntityTypes()) {
            if(type.EMPTY_LOGIC) continue;

            for(ServerEntity entity : getEntitiesOf(type)) {
                entity.tick(delta);

                if(!entity.staticPosition) {
                    entity.changedChunk = false;
                    int chunkX = ExpoShared.posToChunk(entity.posX);
                    int chunkY = ExpoShared.posToChunk(entity.posY);

                    if(entity.forceChunkChange || entity.chunkX != chunkX || entity.chunkY != chunkY) {
                        entity.forceChunkChange = false;
                        entity.chunkX = chunkX;
                        entity.chunkY = chunkY;
                        entity.changedChunk = true;
                        entity.onChunkChanged();
                        justSwitchedChunksEntityList.add(entity);
                    }
                }
            }
        }

        runItemMerge(delta);
    }

    private void runItemMerge(float delta) {
        mergeItemMap.clear();
        List<ServerEntity> itemEntities = typeEntityListMap.get(ServerEntityType.ITEM);
        if(itemEntities.isEmpty()) return;

        for(ServerEntity itemEntity : itemEntities) {
            ServerItem item = (ServerItem) itemEntity;

            if(item.pickupImmunity > 0) {
                continue;
            }

            // ################################################################################### PLAYER HOOK START
            ServerPlayer closestPlayer = getClosestPlayer(item, 20.0f);

            if(closestPlayer != null) {
                if(closestPlayer.itemCooldown) {
                    continue;
                }
                int lastId = closestPlayer.getCurrentItemId();

                int total = item.itemContainer.itemAmount;
                var result = closestPlayer.playerInventory.addItem(item.itemContainer);
                ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(result.changeResult, PacketReceiver.player(closestPlayer));

                if(result.changeResult.changePresent) {
                    ServerPackets.p24PositionalSound("pop", item.posX, item.posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(item));
                    ServerPackets.p38PlayerAnimation(closestPlayer.entityId, ExpoShared.PLAYER_ANIMATION_ID_PICKUP, PacketReceiver.whoCanSee(closestPlayer));

                    if(result.fullTransfer) {
                        ServerPackets.p36PlayerReceiveItem(new int[] {item.itemContainer.itemId}, new int[] {total}, PacketReceiver.player(closestPlayer));
                        item.killEntityWithPacket();
                    } else {
                        item.itemContainer.itemAmount = result.remainingAmount;
                        ServerPackets.p36PlayerReceiveItem(new int[] {item.itemContainer.itemId}, new int[] {total - result.remainingAmount}, PacketReceiver.player(closestPlayer));
                        ServerPackets.p30EntityDataUpdate(item.entityId, new Object[] {item.itemContainer.itemAmount, true}, PacketReceiver.whoCanSee(item));
                    }

                    if(lastId != closestPlayer.getCurrentItemId()) {
                        closestPlayer.heldItemPacket(PacketReceiver.whoCanSee(closestPlayer));
                    }
                }
                continue;
            }
            // ################################################################################### PLAYER HOOK END

            int itemId = item.itemContainer.itemId;
            int maxStackSize = ItemMapper.get().getMapping(item.itemContainer.itemId).logic.maxStackSize;

            if(maxStackSize == item.itemContainer.itemAmount) {
                continue;
            }

            mergeItemMap.computeIfAbsent(itemId, k -> new LinkedList<>());
            mergeItemMap.get(itemId).add(item);
            item.blockedForMerge = false;
        }

        for(Map.Entry<Integer, List<ServerItem>> entrySet : mergeItemMap.entrySet()) {
            List<ServerItem> itemsInCategory = mergeItemMap.get(entrySet.getKey());

            for(ServerItem item : itemsInCategory) {
                var match = findClosestItem(item, itemsInCategory);

                if(match != null) {
                    ServerItem target = match.key;
                    float dst = match.value;

                    if(dst <= 3.0f) {
                        if(!item.blockedForMerge && !target.blockedForMerge) {
                            // Merging distance.
                            item.blockedForMerge = true;
                            target.blockedForMerge = true;

                            int maxTransfer = ItemMapper.get().getMapping(item.itemContainer.itemId).logic.maxStackSize;
                            int possibleTransfer = maxTransfer - item.itemContainer.itemAmount;
                            int availableTransfer = target.itemContainer.itemAmount;

                            if(availableTransfer <= possibleTransfer) {
                                // Transfer fully, delete other entity
                                item.itemContainer.itemAmount += availableTransfer;
                                target.killEntityWithPacket(EntityRemovalReason.MERGE);
                            } else {
                                // Transfer partially, update both stacks
                                item.itemContainer.itemAmount += possibleTransfer;
                                target.itemContainer.itemAmount -= possibleTransfer;
                                ServerPackets.p30EntityDataUpdate(target.entityId, new Object[] {target.itemContainer.itemAmount, false}, PacketReceiver.whoCanSee(target));
                                target.lifetime = 0;
                            }

                            ServerPackets.p30EntityDataUpdate(item.entityId, new Object[] {item.itemContainer.itemAmount, false}, PacketReceiver.whoCanSee(item));
                            item.lifetime = 0;
                            ServerPackets.p24PositionalSound("pop", item);
                        }
                    } else {
                        // Approximation distance.
                        Vector2 v = new Vector2(target.posX, target.posY).sub(item.posX, item.posY).nor();
                        float mergeSpeed = 12.0f;

                        var result = item.physicsBody.move(v.x * delta * mergeSpeed, v.y * delta * mergeSpeed, PhysicsBoxFilters.playerCollisionFilter);

                        item.posX = result.goalX - item.physicsBody.xOffset;
                        item.posY = result.goalY - item.physicsBody.yOffset;

                        ServerPackets.p6EntityPosition(item.entityId, item.posX, item.posY, PacketReceiver.whoCanSee(item));
                    }
                }
            }
        }
    }

    public void spawnItemSingle(float originX, float originY, float originOffsetRadius, String itemName, float moveToRadius) {
        Vector2 position = GenerationUtils.circularRandom(moveToRadius);
        Vector2 offset = GenerationUtils.circularRandom(originOffsetRadius);

        ServerItem item = new ServerItem();

        ItemMapping r = ItemMapper.get().getMapping(itemName);
        item.itemContainer = new ServerInventoryItem(r.id, 1);
        item.posX = originX + offset.x;
        item.posY = originY + offset.y;
        item.dstX = position.x;
        item.dstY = position.y;

        ServerWorld.get().registerServerEntity(dimension.dimensionName, item);
    }

    private Pair<ServerItem, Float> findClosestItem(ServerItem sourceItem, List<ServerItem> categoryItems) {
        ServerItem found = null;
        float dis = Float.MAX_VALUE;
        float MAX_DIS = 20.0f;

        for(ServerItem item : categoryItems) {
            if(sourceItem.entityId == item.entityId) continue;
            float dst = Vector2.dst(item.posX, item.posY, sourceItem.posX, sourceItem.posY);

            if(dst <= MAX_DIS) {
                if(dst < dis) {
                    dis = dst;
                    found = item;
                }
            }
        }

        if(found == null) return null;
        return new Pair<>(found, dis);
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
        justAddedEntityList.add(entity);
        if(entity instanceof DamageableEntity) {
            damageableEntityMap.put(entity.entityId, entity);
        }
        entity.onCreation();
    }

    /** Removes the ServerEntity from the storage maps without modifying them. */
    private void removeEntityUnsafely(ServerEntity entity) {
        idEntityMap.remove(entity.entityId);
        typeEntityListMap.get(entity.getEntityType()).remove(entity);
        justRemovedEntityList.add(entity);
        if(entity instanceof DamageableEntity) {
            damageableEntityMap.remove(entity.entityId);
        }
        entity.onDeletion();
    }

    /** Returns all entities of the same type in the current dimension. */
    public ArrayList<ServerEntity> getEntitiesOf(ServerEntityType type) {
        return typeEntityListMap.get(type);
    }

    /** Returns all player entities in the current dimension. */
    public ArrayList<ServerPlayer> getAllPlayers() {
        ArrayList<ServerEntity> list = getEntitiesOf(ServerEntityType.PLAYER);
        ArrayList<ServerPlayer> copy = new ArrayList<>(list.size());

        for(ServerEntity e : list) {
            copy.add((ServerPlayer) e);
        }

        return copy;
    }

    /** Returns all damageable entities in the current dimension. */
    public Collection<ServerEntity> getAllDamageableEntities() {
        return damageableEntityMap.values();
    }

    /** Returns the closest player to specified entity. */
    public ServerPlayer getClosestPlayer(ServerEntity to, float maxDistance) {
        float dis = Float.MAX_VALUE;
        ServerPlayer player = null;

        for(ServerPlayer players : getAllPlayers()) {
            float dst = Vector2.dst(players.toFeetCenterX(), players.toFeetCenterY(), to.posX, to.posY);

            if(dst <= maxDistance) {
                if(player == null || dis > dst) {
                    dis = dst;
                    player = players;
                }
            }
        }

        return player;
    }

    public ServerEntity getClosestEntity(ServerEntity to, float maxDistance, ServerEntityType... types) {
        float dis = Float.MAX_VALUE;
        ServerEntity entity = null;

        for(ServerEntityType type : types) {
            for(ServerEntity e : getEntitiesOf(type)) {
                float dst = Vector2.dst(e.posX, e.posY, to.posX, to.posY);

                if(dst <= maxDistance) {
                    if(entity == null || dis > dst) {
                        dis = dst;
                        entity = e;
                    }
                }
            }
        }

        return entity;
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

    /** Returns all entities in the current dimension that were added in this tick. */
    public ArrayList<ServerEntity> getJustAddedEntities() {
        return justAddedEntityList;
    }

    /** Returns all entities in the current dimension that were removed in this tick. */
    public ArrayList<ServerEntity> getJustRemovedEntities() {
        return justRemovedEntityList;
    }

    /** Returns all entities in the current dimension that changed chunks in this tick. */
    public ArrayList<ServerEntity> getJustSwitchedChunksEntities() {
        return justSwitchedChunksEntityList;
    }

    /** Returns the entity operation queue in the current dimension. */
    public ConcurrentLinkedQueue<EntityOperation> getEntityOperationQueue() {
        return entityOperationQueue;
    }

}
