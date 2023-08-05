package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.DamageableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

public class ServerFenceStick extends ServerEntity {

    public int fenceOrientation; // [0-15 auto-tiling]
    public EntityPhysicsBox physicsBody;

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.FENCE_STICK;
    }

    public static final float[][] HITBOX = new float[][] {
            new float[] {5, 3, 6, 3},
            new float[] {6, 3, 4, 12},
            new float[] {5, 3, 12, 3},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
            new float[] {},
    };

    @Override
    public void onCreation() {
        calculateOrientation(true, false);
    }

    public void calculateOrientation(boolean updateNeighbours, boolean sendUpdatePacket) {
        // Check neighbouring tiles
        ServerTile tile = getChunkGrid().getTile(ExpoShared.posToTile(posX), ExpoShared.posToTile(posY));
        ServerTile[] neighbours = tile.getNeighbouringTilesNESW();

        int orientation = 0;

        ServerEntity north = neighbours[0].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity east = neighbours[1].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity south = neighbours[2].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity west = neighbours[3].hasTileBasedEntity(ServerEntityType.FENCE_STICK);

        if(north != null) {
            orientation += 1;
            if(updateNeighbours) {
                ((ServerFenceStick) north).calculateOrientation(false, true);
            }
        }
        if(east != null) {
            orientation += 2;
            if(updateNeighbours) {
                ((ServerFenceStick) east).calculateOrientation(false, true);
            }
        }
        if(south != null) {
            orientation += 4;
            if(updateNeighbours) {
                ((ServerFenceStick) south).calculateOrientation(false, true);
            }
        }
        if(west != null) {
            orientation += 8;
            if(updateNeighbours) {
                ((ServerFenceStick) west).calculateOrientation(false, true);
            }
        }

        fenceOrientation = orientation;
        if(sendUpdatePacket) {
            ServerPackets.p30EntityDataUpdate(entityId, getPacketPayload(), PacketReceiver.whoCanSee(this));
        }

        if(physicsBody != null) {
            physicsBody.dispose();
        }

        // Adjust values later depending on orientation
        float[] v = HITBOX[fenceOrientation];

        if(v.length > 0) {
            physicsBody = new EntityPhysicsBox(this, v[0], v[1], v[2], v[3]);
        } else {
            physicsBody = new EntityPhysicsBox(this, 0, 0, 12, 12);
        }
    }

    @Override
    public void onDie() {
        // Detach from internal tile entity structure
        detachFromTile(getChunkGrid().getChunk(chunkX, chunkY));

        // Update tile layer
        int tx = ExpoShared.posToTile(posX);
        int ty = ExpoShared.posToTile(posY);
        ServerTile tile = getChunkGrid().getTile(tx, ty);
        ServerTile[] update = tile.getNeighbouringTilesNESW();

        ServerEntity north = update[0].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity east = update[1].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity south = update[2].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity west = update[3].hasTileBasedEntity(ServerEntityType.FENCE_STICK);

        if(north != null) ((ServerFenceStick) north).calculateOrientation(false, true);
        if(east != null) ((ServerFenceStick) east).calculateOrientation(false, true);
        if(south != null) ((ServerFenceStick) south).calculateOrientation(false, true);
        if(west != null) ((ServerFenceStick) west).calculateOrientation(false, true);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("orientation", fenceOrientation);
    }

    @Override
    public void onLoad(JSONObject saved) {
        fenceOrientation = saved.getInt("orientation");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {fenceOrientation};
    }

}
