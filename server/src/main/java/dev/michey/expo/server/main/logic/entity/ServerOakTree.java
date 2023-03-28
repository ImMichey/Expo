package dev.michey.expo.server.main.logic.entity;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;

public class ServerOakTree extends ServerEntity {

    /** Physics body */
    private BoundingBox physicsBody;

    @Override
    public void onCreation() {
        // add physics body of player to world
        physicsBody = new BoundingBox(this, 29, 5, 11, 5.5f);
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();
    }

    public ServerOakTree() {
        health = 50.0f;
        damageableWith = ToolType.AXE;
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void onDamage(ServerEntity damageSource, float damage) {

    }

    @Override
    public void onDie() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.OAK_TREE;
    }

    @Override
    public void onChunkChanged() {

    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
