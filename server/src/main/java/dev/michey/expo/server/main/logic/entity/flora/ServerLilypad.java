package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.util.SpawnItem;
import org.json.JSONObject;

public class ServerLilypad extends ServerEntity {

    public int variant;

    public ServerLilypad() {
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        variant = MathUtils.random(1, 2);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.LILYPAD;
    }

    @Override
    public void onDie() {
        spawnEntitiesAround(0, 5.5f, 6, 10,
                new SpawnItem("item_wheat", 1, 2))
        ;
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