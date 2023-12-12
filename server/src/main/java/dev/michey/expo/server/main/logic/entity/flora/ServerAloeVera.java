package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;

public class ServerAloeVera extends ServerEntity {

    public ServerAloeVera() {
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onDie() {
        spawnItemsAround(2, 3, 0, 0, "item_aloe_vera", 8, 10);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ALOE_VERA;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
