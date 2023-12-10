package dev.michey.expo.server.main.logic.entity.animal;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.ai.entity.BrainModuleFlee;
import dev.michey.expo.server.main.logic.ai.entity.BrainModuleIdle;
import dev.michey.expo.server.main.logic.ai.entity.BrainModuleStroll;
import dev.michey.expo.server.main.logic.ai.entity.EntityBrain;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoTime;

public class ServerFirefly extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;
    public float despawnDelta = -1.0f;

    public ServerFirefly() {
        health = 20.0f;
        persistentEntity = false;
    }

    @Override
    public void onCreation() {
        brain = new EntityBrain(this);
        brain.addBrainModule(new BrainModuleIdle());
        brain.addBrainModule(new BrainModuleStroll());
        brain.addBrainModule(new BrainModuleFlee());

        physicsBody = new EntityPhysicsBox(this, -1.0f, 0, 2, 2);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        if(damageSource instanceof ServerPlayer sp) {
            var item = sp.getCurrentItem();

            if(item.itemId == ItemMapper.get().getMapping("item_insect_net").id) {
                ItemMapping fireflyItem = ItemMapper.get().getMapping("item_firefly");
                var addResult = sp.playerInventory.addItem(new ServerInventoryItem(fireflyItem.id, 1));

                if(addResult.fullTransfer) {
                    sp.useItemDurability(item);
                    ExpoServerBase.get().getPacketReader().convertInventoryChangeResultToPacket(addResult.changeResult, PacketReceiver.player(sp));
                    ServerPackets.p36PlayerReceiveItem(new int[] {fireflyItem.id}, new int[] {1}, PacketReceiver.player(sp));
                    ServerPackets.p24PositionalSound("pop", this);
                    killEntityWithPacket(EntityRemovalReason.CAUGHT);
                    return false;
                }
            }
        }

        brain.notifyAttacked(damageSource, damage);
        return super.onDamage(damageSource, damage);
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        boolean day = ExpoTime.isDay(getDimension().dimensionTime);

        if(day) {
            if(despawnDelta > 0) {
                despawnDelta -= delta;

                if(despawnDelta <= 0) {
                    killEntityWithPacket(EntityRemovalReason.DESPAWN);
                }
            } else {
                despawnDelta = MathUtils.random(10.0f, 30.0f);
            }
        }

        tickKnockback(delta);
        brain.tickBrain(delta);
        applyKnockback(brain);
    }

    @Override
    public void onMoved() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.FIREFLY;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.FIREFLY);
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.LIGHT;
    }

}
