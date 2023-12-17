package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.server.util.SpawnItem;
import org.json.JSONObject;

public class ServerCampfire extends ServerEntity implements PhysicsEntity {

    public float oldBurnDuration;
    public float burnDuration;
    private EntityPhysicsBox physicsBody;

    public ServerCampfire() {
        health = 50.0f;
        setDamageableWith(ToolType.AXE, ToolType.FIST);
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, -12f, 3, 24, 9);
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        burnDuration = Float.MAX_VALUE;
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CAMPFIRE;
    }

    @Override
    public void onInteraction(ServerPlayer player) {
        burnDuration = 30.0f;
    }

    @Override
    public void tick(float delta) {
        float multiplier = isRaining() ? 1.5f : 1.0f;
        burnDuration -= delta * multiplier;

        if((oldBurnDuration > 0 && burnDuration <= 0) || (oldBurnDuration < 0 && burnDuration > 0)) {
            ServerPackets.p30EntityDataUpdate(this);
        }

        oldBurnDuration = burnDuration;
    }

    @Override
    public void onDie() {
        spawnItemsAround(0, 1.875f, 10, 14,
                new SpawnItem("item_oak_log", 1, 2));
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("burn", burnDuration);
    }

    @Override
    public void onLoad(JSONObject saved) {
        burnDuration = saved.getFloat("burn");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {burnDuration};
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public void onMoved() {

    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.HEAVY;
    }

}