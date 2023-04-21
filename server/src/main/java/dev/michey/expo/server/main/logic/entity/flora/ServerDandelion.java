package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerDandelion extends ServerEntity {

    public ServerDandelion() {
        health = 10.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.DANDELION;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
