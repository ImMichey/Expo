package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.entity.BrainModuleFlee;
import dev.michey.expo.server.main.logic.ai.entity.BrainModuleIdle;
import dev.michey.expo.server.main.logic.ai.entity.BrainModuleStroll;
import dev.michey.expo.server.main.logic.ai.entity.EntityBrain;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;

public class ServerMaggot extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain brain;
    public EntityPhysicsBox physicsBody;

    public ServerMaggot() {
        health = getMetadata().getMaxHealth();
    }

    @Override
    public void onCreation() {
        brain = new EntityBrain(this);
        brain.addBrainModule(new BrainModuleIdle());
        brain.addBrainModule(new BrainModuleStroll());
        brain.addBrainModule(new BrainModuleFlee());

        physicsBody = new EntityPhysicsBox(this, -4.5f, 0, 9, 5);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        brain.notifyAttacked(damageSource, damage);
        return super.onDamage(damageSource, damage);
    }

    @Override
    public void onDie() {
        spawnItemsAround(1, 1, 0, 0.25f, "item_maggot", 8);
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        tickKnockback(delta);
        brain.tickBrain(delta);
        applyKnockback();
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
