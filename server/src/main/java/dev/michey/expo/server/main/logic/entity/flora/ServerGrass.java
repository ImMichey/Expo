package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import org.json.JSONObject;

public class ServerGrass extends ServerEntity {

    public int variant;

    public ServerGrass() {
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        variant = MathUtils.random(1, 6);
    }

    @Override
    public void onDie() {
        float yOff = 10.5f;
        float h = 0;

        if(variant == 1) h = 9;
        if(variant == 2) h = 10;
        if(variant == 3) h = 8;
        if(variant == 4) h = 10;
        if(variant == 5) h = 12;
        if(variant == 6) h = 13;

        spawnItemsAround(1, 2, 0, (h - yOff) * 0.5f, "item_grassfiber", 8);
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
