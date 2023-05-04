package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;
import org.json.JSONObject;

public class ServerGrass extends ServerEntity {

    public int variant;

    public ServerGrass() {
        health = 20.0f;
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        variant = MathUtils.random(1, 5);
    }

    @Override
    public void onDie() {
        int grassSpawned = MathUtils.random(1, 2);
        Vector2[] positions = GenerationUtils.positions(grassSpawned, 8.0f);

        for(int i = 0; i < grassSpawned; i++) {
            ServerItem item = new ServerItem();

            ItemMapping r = ItemMapper.get().getMapping("item_grassfiber");
            item.itemContainer = new ServerInventoryItem(r.id, 1);

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
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("variant", variant);
    }

    @Override
    public void onLoad(JSONObject saved) {
        variant = saved.getInt("variant");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant};
    }

}
