package dev.michey.expo.server.main.logic.entity.crop;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class ServerCropWheat extends ServerCrop {

    public static final float CROP_GROWTH_MIN = 120f;
    public static final float CROP_GROWTH_MAX = 180f;

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CROP_WHEAT;
    }

    @Override
    public void onCreation() {
        cropGrowthDelta = MathUtils.random(CROP_GROWTH_MIN, CROP_GROWTH_MAX);
    }

    @Override
    public void tick(float delta) {
        if(cropAge < 5) {
            float multiplier = isRaining() ? 1.5f : 1.0f;
            cropGrowthDelta -= delta * multiplier;

            if(cropGrowthDelta <= 0) {
                cropAge++;
                cropGrowthDelta = MathUtils.random(CROP_GROWTH_MIN, CROP_GROWTH_MAX); // 2 and 3 minutes
                ServerPackets.p30EntityDataUpdate(entityId, getPacketPayload(), PacketReceiver.whoCanSee(this));
            }
        }
    }

}
