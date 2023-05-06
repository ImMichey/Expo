package dev.michey.expo.server.main.logic.ai.module;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;
import dev.michey.expo.util.ExpoShared;

public class AIModuleWalk extends AIModule {

    private Vector2 walkTo;
    private Vector2 dir;
    private float speed;

    public AIModuleWalk(AIState state, float minDuration, float maxDuration, float speed) {
        super(state, minDuration, maxDuration);
        this.speed = speed;
    }

    @Override
    public void tickModule(float delta) {
        var e = getBrain().getEntity();
        float dstX = e.posX + dir.x * delta * speed;
        float dstY = e.posY + dir.y * delta * speed;

        // Check for loaded chunk
        int chunkX = ExpoShared.posToChunk(dstX);
        int chunkY = ExpoShared.posToChunk(dstY);

        if(e.getChunkGrid().isActiveChunk(chunkX, chunkY)) {
            e.posX = dstX;
            e.posY = dstY;
            ServerPackets.p13EntityMove(e.entityId, e.velToPos(dir.x), e.velToPos(dir.y), e.posX, e.posY, PacketReceiver.whoCanSee(e));
        }
    }

    @Override
    public void onStart() {
        float posX = getBrain().getEntity().posX;
        float posY = getBrain().getEntity().posY;

        walkTo = GenerationUtils.circularRandom(1.0f).add(posX, posY);
        dir = walkTo.cpy().sub(posX, posY).nor();
    }

    @Override
    public void onEnd() {
        switchToStateIfPresent(AIState.IDLE);
    }

}
