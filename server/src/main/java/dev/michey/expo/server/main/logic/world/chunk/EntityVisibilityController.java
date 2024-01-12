package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;

import java.util.Arrays;
import java.util.HashSet;

/** This class controls the visibility behaviour for multiple entities between multiple players. */
public class EntityVisibilityController {

    /** The player to handle */
    private final ServerPlayer player;
    private final HashSet<Integer> visibleEntities;

    private int minChunkX;
    private int maxChunkX;
    private int minChunkY;
    private int maxChunkY;

    public EntityVisibilityController(ServerPlayer player) {
        this.player = player;
        visibleEntities = new HashSet<>();
    }

    public void cacheChunkBounds() {
        int rx = ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X;
        int ry = ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y;
        minChunkX = player.chunkX - rx;
        maxChunkX = player.chunkX + rx;
        minChunkY = player.chunkY - ry;
        maxChunkY = player.chunkY + ry;
    }

    /** Called when an entity spawned/moved between chunks and is now in range of the player. */
    public void handleEntity(ServerEntity entity) {
        if(canSee(entity)) {
            boolean added = visibleEntities.add(entity.entityId);

            if(added) {
                if(entity.getEntityType() == ServerEntityType.PLAYER) {
                    ServerPlayer player = (ServerPlayer) entity;
                    ServerPackets.p9PlayerCreate(player, false, PacketReceiver.player(this.player));
                } else {
                    if(entity.getEntityType().ADVANCED_PAYLOAD) {
                        ServerPackets.p29EntityCreateAdvanced(entity, PacketReceiver.player(player));
                    } else {
                        ServerPackets.p2EntityCreate(entity, PacketReceiver.player(player));
                    }
                }
            }
        } else {
            boolean removed = visibleEntities.remove(entity.entityId);

            if(removed) {
                ServerPackets.p4EntityDelete(entity.entityId, EntityRemovalReason.VISIBILITY, PacketReceiver.player(player));
            }
        }
    }

    /** Called when the player moved between chunks and is now potentially out of range of previously seen entities. */
    public void refreshExistingEntities() {
        int[] removePacket = new int[visibleEntities.size()];
        int position = 0;

        for(ServerEntity toHandle : player.getDimension().getEntityManager().getJustRemovedEntities()) {
            if(visibleEntities.contains(toHandle.entityId)) {
                removePacket[position++] = toHandle.entityId;
            }
        }

        if(player.changedChunk) {
            for(int ent : visibleEntities) {
                if(!canSee(player.getDimension().getEntityManager().getEntityById(ent))) {
                    removePacket[position++] = ent;
                }
            }
        }

        if(position > 0) {
            for(int i = 0; i < position; i++) {
                visibleEntities.remove(removePacket[i]); // update visible entities set
            }

            EntityRemovalReason[] removalArray = new EntityRemovalReason[position];
            Arrays.fill(removalArray, EntityRemovalReason.VISIBILITY);
            int[] newRemovePacketArray = Arrays.copyOf(removePacket, position);
            ServerPackets.p8EntityDeleteStack(newRemovePacketArray, removalArray, PacketReceiver.player(player)); // send entity delete packet(s)
        }

        for(ServerEntity entity : player.getDimension().getEntityManager().getAllEntities()) {
            if(entity.entityId == player.entityId) continue;
            handleEntity(entity);
        }
    }

    public void removeTrackedEntity(int entityId) {
        visibleEntities.remove(entityId);
    }

    public boolean canSee(ServerEntity entity) {
        return entity.chunkX >= minChunkX &&
                entity.chunkX <= maxChunkX &&
                entity.chunkY >= minChunkY &&
                entity.chunkY <= maxChunkY;
    }

    public boolean isAlreadyVisible(int entityId) {
        return visibleEntities.contains(entityId);
    }

}