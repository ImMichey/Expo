package dev.michey.expo.server.main.logic.entity;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerGrass extends ServerEntity {

    @Override
    public void tick(float delta) {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.GRASS;
    }

    @Override
    public void onChunkChanged() {

    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
