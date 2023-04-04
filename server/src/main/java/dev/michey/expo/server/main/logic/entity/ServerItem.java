package dev.michey.expo.server.main.logic.entity;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.InventoryFileLoader;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;

public class ServerItem extends ServerEntity {

    public ServerInventoryItem itemContainer;

    @Override
    public void tick(float delta) {

    }

    @Override
    public void onCreation() {

    }

    @Override
    public void onDeletion() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ITEM;
    }

    @Override
    public void onChunkChanged() {

    }

    @Override
    public void onDamage(ServerEntity damageSource, float damage) {

    }

    @Override
    public void onDie() {

    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("item", InventoryFileLoader.itemToStorageObject(itemContainer));
    }

}
