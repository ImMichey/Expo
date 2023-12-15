package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerTorch extends ServerEntity {

    public ServerTorch() {
        health = 10.0f;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.TORCH;
    }

    @Override
    public void onDie() {
        spawnItemSingle(posX, posY, 0, "item_stick", 8);
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}