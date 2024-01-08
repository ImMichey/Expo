package dev.michey.expo.server.main.logic.entity.hostile;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.ai.entity.*;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;

public class ServerSlime extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;

    public ServerSlime() {
        health = getMetadata().getMaxHealth();
        invincibility = 0.0f;
        persistentEntity = false;
    }

    @Override
    public void onCreation() {
        brain = new EntityBrain(this);
        brain.addBrainModule(new BrainModuleIdleHostile());
        brain.addBrainModule(new BrainModuleHopHostile());
        brain.addBrainModule(new BrainModuleChaseHop());
        brain.addBrainModule(new BrainModuleAttack());

        physicsBody = new EntityPhysicsBox(this, -5.5f, 0, 11, 9);
    }

    @Override
    public void onDie() {
        if(MathUtils.random() <= 0.5f) {
            spawnItemsAround(1, 2, 0, 0, "item_oak_log", 8f);
        }
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public String getImpactSound() {
        return "bloody_squish";
    }

    @Override
    public void tick(float delta) {
        tickKnockback(delta);
        brain.tickBrain(delta);
        applyKnockback(brain);
    }

    @Override
    public void onMoved() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.SLIME;
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.SLIME);
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
