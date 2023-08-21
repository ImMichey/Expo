package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

public class ServerFenceStick extends ServerEntity {

    public int fenceOrientation; // [0-15 auto-tiling]
    public EntityPhysicsBox[] physicsBodies;

    public ServerFenceStick() {
        health = 40.0f;
        setDamageableWith(ToolType.AXE, ToolType.FIST);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.FENCE_STICK;
    }

    public static final float[][] HITBOX = new float[][] {
            new float[] {5, 3, 6, 3},
            new float[] {6, 3, 4, 13},
            new float[] {5, 3, 12, 3},
            new float[] {6, 3, 11, 3, 6, 6, 4, 10},      // 3
            new float[] {6, 0, 4, 6},
            new float[] {6, 0, 4, 16},
            new float[] {6, 3, 10, 3, 6, 0, 4, 6},
            new float[] {6, 0, 4, 16, 6, 3, 10, 3},
            new float[] {0, 3, 11, 3},             // 8
            new float[] {0, 3, 10, 3, 6, 6, 4, 10},
            new float[] {0, 3, 16, 3},
            new float[] {0, 3, 16, 3, 6, 3, 4, 13},
            new float[] {0, 3, 10, 3, 6, 0, 4, 6},
            new float[] {6, 0, 4, 16, 0, 3, 10, 3},
            new float[] {0, 3, 16, 3, 6, 0, 4, 6},
            new float[] {0, 3, 16, 3, 6, 0, 4, 16},
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

        if(neighbours[0] == null) {
            orientation += 1;
        }
        if(neighbours[1] == null) {
            orientation += 2;
        }
        if(neighbours[2] == null) {
            orientation += 4;
        }
        if(neighbours[3] == null) {
            orientation += 8;
        }

        ServerEntity north = neighbours[0] == null ? null : neighbours[0].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity east = neighbours[1] == null ? null : neighbours[1].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity south = neighbours[2] == null ? null : neighbours[2].hasTileBasedEntity(ServerEntityType.FENCE_STICK);
        ServerEntity west = neighbours[3] == null ? null : neighbours[3].hasTileBasedEntity(ServerEntityType.FENCE_STICK);

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

        if(physicsBodies != null) {
            for(var b : physicsBodies) b.dispose();
        }

        // Adjust values later depending on orientation
        float[] v = HITBOX[fenceOrientation];

        if(v.length > 0) {
            physicsBodies = new EntityPhysicsBox[v.length / 4];

            for(int i = 0; i < physicsBodies.length; i++) {
                physicsBodies[i] = new EntityPhysicsBox(this, v[i * 4], v[i * 4 + 1], v[i * 4 + 2], v[i * 4 + 3]);
            }
        } else {
            physicsBodies = new EntityPhysicsBox[] { new EntityPhysicsBox(this, 0, 0, 12, 12) };
        }
    }

    @Override
    public void onDie() {
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

        spawnItemSingle(posX + 8, posY + 8 - 5.625f, 0, "item_fence_stick", 8);

        ServerPackets.p38TileEntityIdUpdate(tile, PacketReceiver.whoCanSee(tile));
    }

    @Override
    public void onDeletion() {
        for(EntityPhysicsBox box : physicsBodies) box.dispose();
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
