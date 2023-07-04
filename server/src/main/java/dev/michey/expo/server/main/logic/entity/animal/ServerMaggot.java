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

public class ServerMaggot extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain maggotBrain = new EntityBrain(this);
    public EntityPhysicsBox physicsBody;

    public ServerMaggot() {
        health = 40.0f;
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -4.5f, 0, 9, 5);
        maggotBrain.addModule(new AIModuleIdle(AIState.IDLE, 2.0f, 6.0f));
        maggotBrain.addModule(new AIModuleWalk(AIState.WALK, 2.0f, 6.0f, 5.0f));
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public void onDie() {
        spawnItemsAround(1, 1, 0, 0.25f, "item_worm", 8);
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        tickKnockback(delta);
        boolean applyKnockback = knockbackAppliedX != 0 || knockbackAppliedY != 0;

        if(applyKnockback) {
            movePhysicsBoxBy(physicsBody, knockbackAppliedX, knockbackAppliedY);
        }

        maggotBrain.tick(delta);

        if(maggotBrain.getCurrentState() != AIState.WALK && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, PacketReceiver.whoCanSee(this));
        }
    }

    @Override
    public void onMoved() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.MAGGOT;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.MAGGOT);
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
