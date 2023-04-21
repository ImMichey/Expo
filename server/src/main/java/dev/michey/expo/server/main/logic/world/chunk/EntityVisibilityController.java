package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.util.ExpoShared.PLAYER_CHUNK_VIEW_RANGE_ONE_DIR;

/** This class controls the visibility behaviour for multiple entities between multiple players. */
public class EntityVisibilityController {

    /** The player to handle */
    private final ServerPlayer player;
    private final HashSet<Integer> visibleEntities;

    public EntityVisibilityController(ServerPlayer player) {
        this.player = player;
        visibleEntities = new HashSet<>();
    }

    /** Called when an entity spawned/moved between chunks and is now in range of the player. */
    public void handleEntity(ServerEntity entity) {
        if(canSee(entity)) {
            if(!visibleEntities.contains(entity.entityId)) {
                visibleEntities.add(entity.entityId);

                if(entity.getEntityType() == ServerEntityType.PLAYER) {
                    ServerPlayer player = (ServerPlayer) entity;
                    ServerPackets.p9PlayerCreate(player, false, PacketReceiver.player(this.player));
                } else {
                    if(entity.getEntityType() == ServerEntityType.ITEM) {
                        ServerInventoryItem item = ((ServerItem) entity).itemContainer;
                        ServerPackets.p29EntityCreateAdvanced(entity, new Object[] {item.itemId, item.itemAmount}, PacketReceiver.player(player));
                    } else {
                        ServerPackets.p2EntityCreate(entity, PacketReceiver.player(player));
                    }
                }
            }
        } else {
            if(visibleEntities.contains(entity.entityId)) {
                ServerPackets.p4EntityDelete(entity.entityId, EntityRemovalReason.VISIBILITY, PacketReceiver.player(player));
                visibleEntities.remove(entity.entityId);
            }
        }
    }

    /** Called when the player moved between chunks and is now potentially out of range of previously seen entities. */
    public void refreshExistingEntities() {
        List<Integer> removePacket = new LinkedList<>();

        for(int entityId : visibleEntities) {
            ServerEntity e = player.getDimension().getEntityManager().getEntityById(entityId);

            // e == null -> If the entity got destroyed on a different way already.
            if(e == null || !canSee(e)) {
                removePacket.add(entityId);
            }
        }

        if(!removePacket.isEmpty()) {
            removePacket.forEach(visibleEntities::remove); // update visible entities set
            int[] array = removePacket.stream().mapToInt(i -> i).toArray(); // convert list to array
            EntityRemovalReason[] removalArray = new EntityRemovalReason[array.length];
            Arrays.fill(removalArray, EntityRemovalReason.VISIBILITY);
            ServerPackets.p8EntityDeleteStack(array, removalArray, PacketReceiver.player(player)); // send entity delete packet(s)
        }

        for(ServerEntityType type : player.getDimension().getEntityManager().getExistingEntityTypes()) {
            for(ServerEntity entity : player.getDimension().getEntityManager().getEntitiesOf(type)) {
                int entityId = entity.entityId;
                if(entityId == player.entityId) continue;

                if(!visibleEntities.contains(entityId)) {
                    handleEntity(entity);
                }
            }
        }
    }

    public void removeTrackedPlayer(int entityId) {
        visibleEntities.remove(entityId);
    }

    public boolean canSee(ServerEntity entity) {
        int range = PLAYER_CHUNK_VIEW_RANGE_ONE_DIR;
        int minChunkX = player.chunkX - range;
        int maxChunkX = player.chunkX + range;
        int minChunkY = player.chunkY - range;
        int maxChunkY = player.chunkY + range;

        return entity.chunkX >= minChunkX &&
                entity.chunkX <= maxChunkX &&
                entity.chunkY >= minChunkY &&
                entity.chunkY <= maxChunkY;
    }

    public boolean isAlreadyVisible(int entityId) {
        return visibleEntities.contains(entityId);
    }

}