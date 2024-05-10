package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.util.SpawnItem;
import org.json.JSONObject;

public class ServerSunflower extends ServerEntity {

    private int variant;

    public ServerSunflower() {
        variant = 1;
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onDie() {
        spawnItemsAround(0, 0, 8, 10, new SpawnItem("item_sunflower", 1, 1));
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        variant = 1;//rnd.random(1, 3);
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant};
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.SUNFLOWER;
    }

    @Override
    public void onLoad(JSONObject saved) {
        variant = saved.getInt("variant");
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("variant", variant);
    }

}
