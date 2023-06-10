package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerSunflower extends ServerEntity {

    public ServerSunflower() {
        health = 20.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.SUNFLOWER;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
