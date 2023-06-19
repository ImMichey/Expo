package dev.michey.expo.server.main.logic.ai.module;

import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;

public class AIModuleIdle extends AIModule {

    public AIModuleIdle(AIState state, float minDuration, float maxDuration) {
        super(state, minDuration, maxDuration);
    }

    @Override
    public void tickModule(float delta) {

    }

    @Override
    public void onStart() {
        var e = getBrain().getEntity();
        ServerPackets.p13EntityMove(e.entityId, 0, 0, e.posX, e.posY, PacketReceiver.whoCanSee(e));
    }

    @Override
    public void onEnd() {
        switchToStateIfPresent(AIState.WALK, AIState.FLY);
    }

}
