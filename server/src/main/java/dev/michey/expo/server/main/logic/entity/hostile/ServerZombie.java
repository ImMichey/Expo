package dev.michey.expo.server.main.logic.entity.hostile;

import com.badlogic.gdx.math.MathUtils;
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

public class ServerZombie extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain zombieBrain = new EntityBrain(this);
    public EntityPhysicsBox physicsBody;

    public ServerZombie() {
        health = 100.0f;
        invincibility = 0.0f;
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -3, 0, 6, 5);

        zombieBrain.addModule(new AIModuleIdle(AIState.IDLE, 3.0f, 8.0f));
        zombieBrain.addModule(new AIModuleWalk(AIState.WALK, 4.0f, 8.0f, 16f));
    }

    @Override
    public void onDie() {
        if(MathUtils.random() <= 0.2f) {
            spawnItemsAround(1, 2, 0, 0, "item_maggot", 8f);
        }
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public void tick(float delta) {
        tickKnockback(delta);
        boolean applyKnockback = knockbackAppliedX != 0 || knockbackAppliedY != 0;

        if(applyKnockback) {
            movePhysicsBoxBy(physicsBody, knockbackAppliedX, knockbackAppliedY);
        }

        zombieBrain.tick(delta);

        if(zombieBrain.getCurrentState() != AIState.WALK && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, PacketReceiver.whoCanSee(this));
        }
    }

    @Override
    public void onMoved() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ZOMBIE;
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.ZOMBIE);
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.MEDIUM;
    }

}
