package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.util.EntityMetadata;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class BrainModuleIdle extends BrainModule {

    private float minDuration, maxDuration;
    private float remainingDuration;

    @Override
    public void init() {
        EntityMetadata meta = getBrain().getEntity().getMetadata();
        minDuration = meta.getFloat("ai.idleMin");
        maxDuration = meta.getFloat("ai.idleMax");
    }

    @Override
    public void onStart() {
        remainingDuration = MathUtils.random(minDuration, maxDuration);
        getBrain().resetMovementPacket();
    }

    @Override
    public void onEnd() {

    }

    @Override
    public void tick(float delta) {
        remainingDuration -= delta;

        if(remainingDuration <= 0) {
            getBrain().setActiveModuleIfExisting(AIConstants.STROLL);
        }
    }

    @Override
    public int getType() {
        return AIConstants.IDLE;
    }

}
