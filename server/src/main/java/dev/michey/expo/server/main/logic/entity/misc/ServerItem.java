package dev.michey.expo.server.main.logic.entity.misc;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerItem extends ServerEntity {

    public ServerInventoryItem itemContainer;
    public float dstX;
    public float dstY;
    public float originX, originY;
    public float dstDelta;
    public float pickupImmunity = 1.0f;
    public float lifetime;

    // Merge logic
    private int mergeEntityId = -1;
    private boolean skipStackThisTick = false;

    @Override
    public void tick(float delta) {
        lifetime += delta;

        if(lifetime >= 300f) {
            // 5 minutes
            killEntityWithPacket();
            return;
        }

        float MAX_DELTA = 0.5f;

        if(dstDelta != MAX_DELTA && dstX != 0) {
            dstDelta += delta;
            if(dstDelta > MAX_DELTA) dstDelta = MAX_DELTA;

            float alpha = Interpolation.exp10Out.apply(dstDelta);
            posX = originX + alpha * dstX;
            posY = originY + alpha * dstY;

            ServerPackets.p13EntityMove(entityId, (int) dstX, (int) dstY, posX, posY, PacketReceiver.whoCanSee(this));
        }

        if(pickupImmunity > 0) {
            pickupImmunity -= delta;
        }

        if(pickupImmunity <= 0) {
            ServerPlayer closestPlayer = getDimension().getEntityManager().getClosestPlayer(this, 8.0f);

            if(closestPlayer != null) {
                int lastId = closestPlayer.getCurrentItemId();

                var result = closestPlayer.playerInventory.addItem(itemContainer);
                ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(result.changeResult, PacketReceiver.player(closestPlayer));

                if(result.changeResult.changePresent) {
                    ServerPackets.p24PositionalSound("pop", posX, posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));

                    if(result.fullTransfer) {
                        killEntityWithPacket();
                    } else {
                        itemContainer.itemAmount = result.remainingAmount;
                        ServerPackets.p30EntityDataUpdate(entityId, new Object[] {itemContainer.itemAmount, true}, PacketReceiver.whoCanSee(this));
                    }

                    if(lastId != closestPlayer.getCurrentItemId()) {
                        closestPlayer.heldItemPacket(PacketReceiver.whoCanSee(this));
                    }
                }
            } else {
                if(!skipStackThisTick) {
                    boolean findNewMergeItem = true;
                    ServerItem mergeEntity = null;

                    if(mergeEntityId != -1) {
                        // Already found merge entity, double-check its existence
                        ServerEntity checked = getDimension().getEntityManager().getEntityById(mergeEntityId);

                        if(checked != null) {
                            findNewMergeItem = false;
                            mergeEntity = (ServerItem) checked;
                        } else {
                            mergeEntityId = -1;
                        }
                    }

                    if(findNewMergeItem) {
                        // Attempt merging
                        ServerItem mergeWith = findMergeEntity();

                        if(mergeWith != null) {
                            mergeEntityId = mergeWith.entityId;
                            mergeEntity = mergeWith;
                        }
                    }

                    if(mergeEntity != null) {
                        float dstNow = Vector2.dst(mergeEntity.posX, mergeEntity.posY, posX, posY);

                        if(dstNow <= 3.0f) {
                            // stack.
                            mergeEntity.skipStackThisTick = true;

                            int maxTransfer = ItemMapper.get().getMapping(itemContainer.itemId).logic.maxStackSize;
                            int possibleTransfer = maxTransfer - itemContainer.itemAmount;
                            int availableTransfer = mergeEntity.itemContainer.itemAmount;

                            if(availableTransfer <= possibleTransfer) {
                                // Transfer fully, delete other entity
                                itemContainer.itemAmount += availableTransfer;
                                mergeEntity.killEntityWithPacket(EntityRemovalReason.MERGE);
                            } else {
                                // Transfer partially, update both stacks
                                itemContainer.itemAmount += possibleTransfer;
                                mergeEntity.itemContainer.itemAmount -= possibleTransfer;
                                ServerPackets.p30EntityDataUpdate(mergeEntity.entityId, new Object[] {mergeEntity.itemContainer.itemAmount, false}, PacketReceiver.whoCanSee(mergeEntity));
                                mergeEntity.lifetime = 0;
                            }

                            ServerPackets.p30EntityDataUpdate(entityId, new Object[] {itemContainer.itemAmount, false}, PacketReceiver.whoCanSee(this));
                            lifetime = 0;
                            ServerPackets.p24PositionalSound("pop", this);
                        } else {
                            Vector2 v = new Vector2(mergeEntity.posX, mergeEntity.posY).sub(posX, posY).nor();
                            float mergeSpeed = 12.0f;

                            posX += v.x * delta * mergeSpeed;
                            posY += v.y * delta * mergeSpeed;

                            ServerPackets.p6EntityPosition(entityId, posX, posY, PacketReceiver.whoCanSee(this));
                        }
                    }
                } else {
                    skipStackThisTick = false;
                }
            }
        }
    }

    private ServerItem findMergeEntity() {
        // Make sure to cancel algorithm when its not stackable
        ItemMapping mapping = ItemMapper.get().getMapping(itemContainer.itemId);
        if(itemContainer.itemAmount == mapping.logic.maxStackSize) return null;

        List<ServerEntity> items = getDimension().getEntityManager().getEntitiesOf(ServerEntityType.ITEM);
        ServerItem found = null;
        float dis = Float.MAX_VALUE;
        float MAX_DIS = 20.0f;

        for(ServerEntity se : items) {
            if(se.entityId == entityId) continue;
            ServerItem item = (ServerItem) se;
            if(item.pickupImmunity > 0) continue;

            // If found item is already fully stacked, ignore
            if(item.itemContainer.itemAmount == mapping.logic.maxStackSize) continue;

            float dst = Vector2.dst(item.posX, item.posY, posX, posY);

            if(dst <= MAX_DIS) {
                if(dst < dis) {
                    dis = dst;
                    found = item;
                }
            }
        }

        return found;
    }

    @Override
    public void onCreation() {
        originX = posX;
        originY = posY;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ITEM;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("lifetime", lifetime).add("item", InventoryFileLoader.itemToStorageObject(itemContainer));
    }

    @Override
    public void onLoad(JSONObject saved) {
        ServerInventoryItem item = new ServerInventoryItem();
        InventoryFileLoader.loadItemFromStorage(item, saved.getJSONObject("item"));
        itemContainer = item;

        lifetime = saved.getFloat("lifetime");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {itemContainer.itemId, itemContainer.itemAmount};
    }

}
