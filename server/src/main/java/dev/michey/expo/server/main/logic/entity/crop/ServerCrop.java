package dev.michey.expo.server.main.logic.entity.crop;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import org.json.JSONObject;

public abstract class ServerCrop extends ServerEntity {

    public int cropAge;
    public float cropGrowthDelta;

    public ServerCrop() {
        setDamageableWith(ToolType.FIST, ToolType.SCYTHE);
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {cropAge, cropGrowthDelta};
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("cropAge", cropAge).add("cropGrowthDelta", cropGrowthDelta);
    }

    @Override
    public void onLoad(JSONObject saved) {
        cropAge = saved.getInt("cropAge");
        cropGrowthDelta = saved.getFloat("cropGrowthDelta");
    }

}
