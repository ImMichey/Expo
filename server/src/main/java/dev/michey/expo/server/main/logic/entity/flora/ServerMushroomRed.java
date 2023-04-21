package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;

public class ServerMushroomRed extends ServerEntity {

    public ServerMushroomRed() {
        health = 10.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.MUSHROOM_RED;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
