package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.server.main.logic.ai.module.AIModuleIdle;
import dev.michey.expo.server.main.logic.ai.module.AIModuleWalk;
import dev.michey.expo.server.main.logic.entity.arch.*;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;
import org.json.JSONObject;

public class ServerChicken extends ServerEntity implements DamageableEntity, PhysicsEntity {

    public EntityBrain wormBrain = new EntityBrain(this);
    public EntityPhysicsBox physicsBody;

    public int variant;

    public ServerChicken() {
        health = 40.0f;
        variant = 2;
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -4.5f, 0, 9, 5);
        wormBrain.addModule(new AIModuleWalk(AIState.WALK, 1.0f, 4.0f, 10.0f));
        wormBrain.addModule(new AIModuleIdle(AIState.IDLE, 2.0f, 7.0f));
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        //variant = rnd.random(1, 2);
        variant = 2;
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

        wormBrain.tick(delta);

        if(wormBrain.getCurrentState() != AIState.WALK && applyKnockback) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, 0, PacketReceiver.whoCanSee(this));
        }
    }

    @Override
    public void onMoved() {

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
