package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.util.SpawnItem;
import org.json.JSONObject;

public class ServerCampfire extends ServerEntity implements PhysicsEntity {

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
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CAMPFIRE;
    }

    @Override
    public void onDie() {
        spawnItemsAround(0, 1.875f, 10, 14,
                new SpawnItem("item_oak_log", 1, 3));
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