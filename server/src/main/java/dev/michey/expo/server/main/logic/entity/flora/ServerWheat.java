package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.SpawnItem;

public class ServerWheat extends ServerEntity {

    public ServerWheat() {
        health = 20.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.WHEAT_PLANT;
    }

    @Override
    public void onDie() {
        spawnItemsAround(0, 5.5f, 6, 10,
                new SpawnItem("item_wheat", 1, 2));
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}