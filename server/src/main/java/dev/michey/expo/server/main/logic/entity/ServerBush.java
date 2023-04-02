package dev.michey.expo.server.main.logic.entity;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerBush extends ServerEntity {

    @Override
    public void onCreation() {

    }

    @Override
    public void onDeletion() {

    }

    public ServerBush() {
        health = 40.0f;
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
        return ServerEntityType.BUSH;
    }

    @Override
    public void onChunkChanged() {

    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
