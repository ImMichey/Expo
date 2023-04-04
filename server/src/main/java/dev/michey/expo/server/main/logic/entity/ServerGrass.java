package dev.michey.expo.server.main.logic.entity;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.ServerWorld;

import static dev.michey.expo.log.ExpoLogger.log;

public class ServerGrass extends ServerEntity {

    @Override
    public void onCreation() {

    }

    @Override
    public void onDeletion() {

    }

    public ServerGrass() {
        health = 20.0f;
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void onDamage(ServerEntity damageSource, float damage) {

    }

    @Override
    public void onDie() {
        ServerItem item = new ServerItem();
        item.itemContainer = new ServerInventoryItem(ItemMapper.get().getMapping("item_blueberry").id, MathUtils.random(1, 4));
        item.posX = posX;
        item.posY = posY;
        ServerWorld.get().registerServerEntity(entityDimension, item);
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
