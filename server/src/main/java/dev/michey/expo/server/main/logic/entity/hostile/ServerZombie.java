package dev.michey.expo.server.main.logic.entity.hostile;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.ai.ServerZombieBrain;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class ServerZombie extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityPhysicsBox physicsBody;

    public ServerZombieBrain brain = new ServerZombieBrain(this);

    public ServerZombie() {
        health = 100.0f;
        invincibility = 0.0f;
        persistentEntity = false;
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -3, 0, 6, 5);
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

        brain.tick(delta);

        if(brain.currentMode != AIConstants.STROLL && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, 0, PacketReceiver.whoCanSee(this));
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
