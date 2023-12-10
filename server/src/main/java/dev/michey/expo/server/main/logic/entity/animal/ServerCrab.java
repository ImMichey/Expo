package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.entity.*;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;

public class ServerCrab extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;
    private int variant;

    public ServerCrab() {
        variant = 1;
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
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        if(rnd.random(8) == 1) {
            variant = 2;
        } else {
            variant = 1;
        }
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        tickKnockback(delta);
        brain.tickBrain(delta);
        applyKnockback(brain);
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
    public Object[] getPacketPayload() {
        return new Object[] {variant};
    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.MEDIUM_PLAYER_PASSABLE;
    }

}
