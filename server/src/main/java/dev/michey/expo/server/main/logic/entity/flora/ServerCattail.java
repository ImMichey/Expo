package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import org.json.JSONObject;

public class ServerCattail extends ServerEntity {

    public int variant;

    public ServerCattail() {
        variant = 1;
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        variant = rnd.random(2, 4);

        /*
        int tx = ExpoShared.posToTile(posX);
        int ty = ExpoShared.posToTile(posY);

        posX = ExpoShared.tileToPos(tx) + 8f + rnd.random(-1f, 1f);
        posY = ExpoShared.tileToPos(ty) + 8f + rnd.random(-1f, 1f);
        */
    }

    @Override
    public void onDie() {
        spawnItemsAround(2, 3, 0, 6, "item_grassfiber", 8);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CATTAIL;
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
