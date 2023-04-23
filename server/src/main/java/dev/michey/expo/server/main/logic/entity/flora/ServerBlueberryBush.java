package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

public class ServerBlueberryBush extends ServerEntity {

    public boolean hasBerries;
    public float berryRegrowthDelta;

    public ServerBlueberryBush() {
        health = 40.0f;
    }

    @Override
    public void onGeneration() {
        hasBerries = MathUtils.randomBoolean();
        berryRegrowthDelta = MathUtils.random(180f, 360f); // 3-6 min
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.BLUEBERRY_BUSH;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("berries", hasBerries).add("regrowth", berryRegrowthDelta);
    }

    @Override
    public void onLoad(JSONObject saved) {
        hasBerries = saved.getBoolean("berries");
        berryRegrowthDelta = saved.getFloat("regrowth");
    }

}
