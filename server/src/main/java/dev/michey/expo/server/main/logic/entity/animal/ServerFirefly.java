package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.server.main.logic.ai.module.AIModuleFly;
import dev.michey.expo.server.main.logic.ai.module.AIModuleIdle;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;

public class ServerFirefly extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain fireflyBrain = new EntityBrain(this);
    public EntityPhysicsBox physicsBody;

    public ServerFirefly() {
        health = 20.0f;
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -1.0f, 0, 2, 2);
        fireflyBrain.addModule(new AIModuleIdle(AIState.IDLE, 0.25f, 0.75f));
        fireflyBrain.addModule(new AIModuleFly(AIState.FLY, 0.8f, 2.0f, 48.0f));
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        tickKnockback(delta);
        boolean applyKnockback = knockbackAppliedX != 0 || knockbackAppliedY != 0;

        if(applyKnockback) {
            movePhysicsBoxBy(physicsBody, knockbackAppliedX, knockbackAppliedY);
        }

        fireflyBrain.tick(delta);

        if(fireflyBrain.getCurrentState() != AIState.FLY && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, PacketReceiver.whoCanSee(this));
        }
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
