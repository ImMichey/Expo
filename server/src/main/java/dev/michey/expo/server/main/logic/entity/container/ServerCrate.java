package dev.michey.expo.server.main.logic.entity.container;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;

public class ServerCrate extends ServerEntity {

    public EntityPhysicsBox physicsBody;

    public ServerCrate() {
        health = 50.0f;
        setDamageableWith(ToolType.FIST, ToolType.AXE);
    }

    @Override
    public void onCreation() {
        physicsBody = new EntityPhysicsBox(this, 1, 0, 14, 10);
    }

    @Override
    public void onDie() {
        spawnItemSingle(posX + 8, posY + 2, 0, "item_crate", 8);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CRATE;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
