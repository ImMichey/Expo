package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
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

    public EntityBrain crabBrain = new EntityBrain(this);
    public EntityPhysicsBox physicsBody;

    public ServerCrab() {
        health = 50.0f;
    }

    @Override
    public void onCreation() {
        crabBrain.addModule(new AIModuleIdle(AIState.IDLE, 4.0f, 8.0f));
        crabBrain.addModule(new AIModuleWalk(AIState.WALK, 3.0f, 6.0f, 8.0f));

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

        crabBrain.tick(delta);

        if(crabBrain.getCurrentState() != AIState.WALK && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, 0, PacketReceiver.whoCanSee(this));
        }
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
