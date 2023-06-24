package dev.michey.expo.server.main.logic.entity.animal;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.server.main.logic.ai.module.firefly.AIModuleFireflyFly;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoTime;

public class ServerFirefly extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain fireflyBrain = new EntityBrain(this);
    public EntityPhysicsBox physicsBody;
    public float despawnDelta = -1.0f;

    public ServerFirefly() {
        health = 20.0f;
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -1.0f, 0, 2, 2);
        fireflyBrain.addModule(new AIModuleFireflyFly(AIState.FLY, 0.1f, 1.0f, 24.0f));
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
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
