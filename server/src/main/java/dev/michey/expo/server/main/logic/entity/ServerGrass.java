package dev.michey.expo.server.main.logic.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;

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
        int grassSpawned = MathUtils.random(2, 4);
        Vector2[] positions = GenerationUtils.positions(grassSpawned, 8.0f);

        for(int i = 0; i < grassSpawned; i++) {
            ServerItem item = new ServerItem();

            ItemMapping r = ItemMapper.get().randomMapping();
            item.itemContainer = new ServerInventoryItem(r.id, MathUtils.random(1, r.logic.maxStackSize));

            item.posX = posX + 8f;
            item.posY = posY + 4f;
            item.dstX = positions[i].x;
            item.dstY = positions[i].y;
            ServerWorld.get().registerServerEntity(entityDimension, item);
        }
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
