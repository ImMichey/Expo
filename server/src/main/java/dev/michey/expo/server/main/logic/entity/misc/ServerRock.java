package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerRock extends ServerEntity {

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ROCK;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
