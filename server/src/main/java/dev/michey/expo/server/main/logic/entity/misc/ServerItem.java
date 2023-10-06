package dev.michey.expo.server.main.logic.entity.misc;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

import java.util.List;

public class ServerItem extends ServerEntity implements PhysicsEntity {

    public ServerInventoryItem itemContainer;
    public float dstX;
    public float dstY;
    public float originX, originY;
    public float dstDelta;
    public float pickupImmunity = 0.5f;
    public float lifetime;

    public boolean blockedForMerge = false;

    /** Physics body */
    public EntityPhysicsBox physicsBody;

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
            float toMoveX = originX + alpha * dstX;
            float toMoveY = originY + alpha * dstY;

            var result = physicsBody.moveAbsolute(toMoveX, toMoveY, PhysicsBoxFilters.playerCollisionFilter);

            posX = result.goalX - physicsBody.xOffset;
            posY = result.goalY - physicsBody.yOffset;

            ServerPackets.p13EntityMove(entityId, (int) dstX, (int) dstY, posX, posY, 0, PacketReceiver.whoCanSee(this));
        }

        if(pickupImmunity > 0) {
            pickupImmunity -= delta;
        }
    }

    @Override
    public void onCreation() {
        originX = posX;
        originY = posY;
        physicsBody = new EntityPhysicsBox(this, -2, 0, 4, 4);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
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

    @Override
    public String toString() {
        return "[" + itemContainer.itemAmount + "x " + itemContainer.itemId + "]";
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
        return PhysicsMassClassification.ITEM;
    }

}
