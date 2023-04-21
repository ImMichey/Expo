package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;

public class ServerAncientTree extends ServerEntity {

    /** Physics body */
    private BoundingBox physicsBody;

    @Override
    public void onCreation() {
        // add physics body of player to world
        physicsBody = new BoundingBox(this, 1, 3, 21, 4.5f);
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();
    }

    public ServerAncientTree() {
        health = 100.0f;
        damageableWith = ToolType.AXE;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ANCIENT_TREE;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
