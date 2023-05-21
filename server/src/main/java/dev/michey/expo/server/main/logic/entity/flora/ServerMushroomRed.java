package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerMushroomRed extends ServerEntity {

    public ServerMushroomRed() {
        health = 10.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.MUSHROOM_RED;
    }

    @Override
    public void onDie() {
        spawnItemSingle(posX, posY, 0, "item_mushroom_red", 8);
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
