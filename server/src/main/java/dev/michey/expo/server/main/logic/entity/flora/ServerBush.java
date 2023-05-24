package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.SpawnItem;

public class ServerBush extends ServerEntity {

    public ServerBush() {
        health = 40.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.BUSH;
    }

    @Override
    public void onDie() {
        spawnEntitiesAround(0, 3.25f, 10, 14,
                new SpawnItem("item_stick", 2, 4),
                new SpawnItem("item_grassfiber", 1, 3));
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
