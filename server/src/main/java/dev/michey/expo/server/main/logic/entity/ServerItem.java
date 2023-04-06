package dev.michey.expo.server.main.logic.entity;

import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;

public class ServerItem extends ServerEntity {

    public ServerInventoryItem itemContainer;
    public float dstX;
    public float dstY;
    public float originX, originY;
    public float dstDelta;
    public float pickupImmunity = 1.0f;

    @Override
    public void tick(float delta) {
        float MAX_DELTA = 0.5f;

        if(dstDelta != MAX_DELTA && dstX != 0) {
            dstDelta += delta;
            if(dstDelta > MAX_DELTA) dstDelta = MAX_DELTA;

            float alpha = Interpolation.exp10Out.apply(dstDelta);
            posX = originX + alpha * dstX;
            posY = originY + alpha * dstY;

            ServerPackets.p13EntityMove(entityId, (int) dstX, (int) dstY, posX, posY, PacketReceiver.whoCanSee(this));
        }

        if(pickupImmunity > 0) pickupImmunity -= delta;

        if(pickupImmunity <= 0) {
            ServerPlayer closestPlayer = getDimension().getEntityManager().getClosestPlayer(this, 8.0f);

            if(closestPlayer != null) {
                var result = closestPlayer.playerInventory.addItem(itemContainer);
                ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(result.changeResult, PacketReceiver.player(closestPlayer));

                if(result.changeResult.changePresent) {
                    ServerPackets.p24PositionalSound("pop", posX, posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));

                    if(result.fullTransfer) {
                        killEntityWithPacket();
                    } else {
                        itemContainer.itemAmount = result.remainingAmount;
                        ServerPackets.p30EntityDataUpdate(entityId, new Object[] {itemContainer.itemAmount}, PacketReceiver.whoCanSee(this));
                    }
                }
            }
        }
    }

    @Override
    public void onCreation() {
        originX = posX;
        originY = posY;
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ITEM;
    }

    @Override
    public void onChunkChanged() {

    }

    @Override
    public void onDamage(ServerEntity damageSource, float damage) {

    }

    @Override
    public void onDie() {

    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("item", InventoryFileLoader.itemToStorageObject(itemContainer));
    }

}
