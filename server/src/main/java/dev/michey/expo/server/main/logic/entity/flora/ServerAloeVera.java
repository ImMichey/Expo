package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.util.SpawnItem;

public class ServerAloeVera extends ServerEntity {

    public ServerAloeVera() {
        health = 30.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onDie() {
        int seeds = MathUtils.random() <= 0.2f ? 2 : 1;
        int leaves = seeds == 1 ? 2 : 1;

        spawnItemsAround(0, 0, 8, 10, new SpawnItem("item_aloe_vera", leaves, leaves), new SpawnItem("item_aloe_vera_seeds", seeds, seeds));
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
