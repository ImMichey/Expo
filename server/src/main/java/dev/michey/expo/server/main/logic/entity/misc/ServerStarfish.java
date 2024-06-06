package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerStarfish extends ServerEntity {

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.STARFISH;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

    @Override
    public void onDie() {
        spawnItemsAround(1, 1, 2, 2, "item_starfish", 8);
    }
}
