package dev.michey.expo.server.main.logic.entity.hostile;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.ai.entity.*;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;

public class ServerSlimeGreen extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;

    public ServerSlimeGreen() {
        health = getMetadata().getMaxHealth();
        invincibility = 0.0f;
        persistentEntity = false;
    }

    @Override
    public void onCreation() {
        brain = new EntityBrain(this);
        brain.addBrainModule(new BrainModuleIdleHostile());
        brain.addBrainModule(new BrainModuleStrollHostile());
        brain.addBrainModule(new BrainModuleChase());
        brain.addBrainModule(new BrainModuleAttack());

        physicsBody = new EntityPhysicsBox(this, -3, 0, 6, 5);
    }

    @Override
    public void onDie() {
        if(MathUtils.random() <= 0.5f) {
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
        brain.tickBrain(delta);
        applyKnockback();
    }

    @Override
    public void onMoved() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.SLIME_GREEN;
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.SLIME_GREEN);
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
