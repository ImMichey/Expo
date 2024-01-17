package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.entity.*;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import org.json.JSONObject;

public class ServerChicken extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;
    private int variant;

    public ServerChicken() {
        health = getMetadata().getMaxHealth();
        variant = 2;
        resetInvincibility();
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -4.5f, 0, 9, 5);

        brain = new EntityBrain(this);
        brain.addBrainModule(new BrainModuleIdle());
        brain.addBrainModule(new BrainModuleStroll());
        brain.addBrainModule(new BrainModuleFlee());
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        //variant = rnd.random(1, 2);
        variant = 2;
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        brain.notifyAttacked(damageSource, damage);
        return super.onDamage(damageSource, damage);
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
        brain.tickBrain(delta);
        applyKnockback(brain);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CHICKEN;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("var", variant);
    }

    @Override
    public void onLoad(JSONObject saved) {
        variant = saved.getInt("var");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant};
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.CHICKEN);
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
