package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrainOld;
import dev.michey.expo.server.main.logic.ai.entity.*;
import dev.michey.expo.server.main.logic.ai.module.AIModuleIdle;
import dev.michey.expo.server.main.logic.ai.module.AIModuleWalk;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;

public class ServerCrab extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;

    public ServerCrab() {
        health = getMetadata().getMaxHealth();
    }

    @Override
    public void onCreation() {
        brain = new EntityBrain(this);
        brain.addBrainModule(new BrainModuleIdleHostile());
        brain.addBrainModule(new BrainModuleStrollHostile());
        brain.addBrainModule(new BrainModuleFlee());
        brain.addBrainModule(new BrainModuleChase());
        brain.addBrainModule(new BrainModuleAttack());

        physicsBody = new EntityPhysicsBox(this, -4.5f, 0, 9, 5);
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        tickKnockback(delta);
        boolean applyKnockback = knockbackAppliedX != 0 || knockbackAppliedY != 0;

        if(applyKnockback) {
            movePhysicsBoxBy(physicsBody, knockbackAppliedX, knockbackAppliedY);
        }

        brain.tickBrain(delta);

        if(applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, 0, PacketReceiver.whoCanSee(this));
        }

        /*
        if(crabBrain.getCurrentState() != AIState.WALK && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, 0, PacketReceiver.whoCanSee(this));
        }
        */
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        brain.notifyAttacked(damageSource, damage);
        return super.onDamage(damageSource, damage);
    }

    @Override
    public void onDie() {
        spawnItemsAround(1, 1, 0, 0f, "item_crab_claw", 8);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CRAB;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.CRAB);
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
        return PhysicsMassClassification.LIGHT;
    }

}
