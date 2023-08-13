package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.server.util.SpawnItem;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;
import org.json.JSONObject;

public class ServerDynamic3DTile extends ServerEntity implements PhysicsEntity {

    private EntityPhysicsBox physicsBody;
    public TileLayerType emulatingType;
    public int[] layerIds;

    @Override
    public void onCreation() {
        // add physics body of player to world
        checkForBoundingBox();

        if(emulatingType == TileLayerType.ROCK) {
            setDamageableWith(ToolType.PICKAXE);
            health = 50.0f;
        }
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        if(physicsBody != null) {
            physicsBody.dispose();
            physicsBody = null;
        }
    }

    public void checkForBoundingBox() {
        if(physicsBody == null) {
            if(hasBoundingBox(layerIds, emulatingType)) {
                physicsBody = new EntityPhysicsBox(this, 0, 0, 16, 16);
            }
        }
    }

    @Override
    public void onDie() {
        // Update tile layer
        int tx = ExpoShared.posToTile(posX);
        int ty = ExpoShared.posToTile(posY);
        ServerTile tile = getChunkGrid().getTile(tx, ty);
        tile.dynamicTileParts[1].update(TileLayerType.EMPTY);
        ServerPackets.p32ChunkDataSingle(tile, 1);

        // Update neighbours
        ServerTile[] neighbours = tile.getNeighbouringTiles();

        for(ServerTile neighbour : neighbours) {
            if(neighbour.chunk.hasTileBasedEntities()) {
                int entityId = neighbour.chunk.getTileBasedEntityIdGrid()[neighbour.tileArray];

                if(entityId != -1) {
                    ServerEntity found = getDimension().getEntityManager().getEntityById(entityId);

                    if(found instanceof ServerDynamic3DTile x) {
                        if(x.emulatingType == emulatingType) {
                            x.layerIds = neighbour.runTextureGrab(x.emulatingType.TILE_ID_DATA[0], 1);
                            x.checkForBoundingBox();
                            ServerPackets.p30EntityDataUpdate(x.entityId, new Object[] {x.layerIds, x.emulatingType.SERIALIZATION_ID}, PacketReceiver.whoCanSee(x));
                        }
                    }
                }
            }
        }

        spawnItemsAround(8, 8, 8, 8, new SpawnItem("item_rock", 2, 3), new SpawnItem("item_flint", 1, 1, 0.5f));
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.DYNAMIC_3D_TILE;
    }

    @Override
    public SavableEntity onSave() {
        JSONArray array = new JSONArray();
        for(int i : layerIds) array.put(i);
        return new SavableEntity(this).pack().add("layerIds", array).add("layerType", emulatingType.SERIALIZATION_ID);
    }

    @Override
    public void onLoad(JSONObject saved) {
        emulatingType = TileLayerType.serialIdToType(saved.getInt("layerType"));
        JSONArray array = saved.getJSONArray("layerIds");
        layerIds = new int[array.length()];

        for(int i = 0; i < array.length(); i++) {
            layerIds[i] = array.getInt(i);
        }
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {layerIds, emulatingType.SERIALIZATION_ID};
    }

    public static boolean hasBoundingBox(int[] layerIds, TileLayerType emulatingType) {
        int first = layerIds[0] - emulatingType.TILE_ID_DATA[0];
        boolean boundingBox = first != 0;

        if(boundingBox) {
            for(int i : layerIds) {
                int adjusted = i - emulatingType.TILE_ID_DATA[0];

                if(adjusted == 0) {
                    boundingBox = false;
                    break;
                }
            }
        }

        return boundingBox;
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public void onMoved() {

    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.WALL;
    }

}
