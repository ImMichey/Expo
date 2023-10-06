package dev.michey.expo.server.main.logic.ai.module;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;
import dev.michey.expo.util.ExpoShared;

public class AIModuleFly extends AIModule {

    private Vector2 walkTo;
    private Vector2 dir;
    private float speed;

    public AIModuleFly(AIState state, float minDuration, float maxDuration, float speed) {
        super(state, minDuration, maxDuration);
        this.speed = speed;
    }

    @Override
    public void tickModule(float delta) {
        var e = getBrain().getEntity();

        if(e instanceof PhysicsEntity pe) {
            EntityPhysicsBox box = pe.getPhysicsBox();

            float toMoveX = dir.x * delta * speed;
            float toMoveY = dir.y * delta * speed;

            float oldPosX = e.posX;
            float oldPosY = e.posY;
            var result = box.move(toMoveX, toMoveY, PhysicsBoxFilters.generalFilter);

            float targetX = result.goalX - box.xOffset;
            float targetY = result.goalY - box.yOffset;

            // Check for loaded chunk
            int chunkX = ExpoShared.posToChunk(targetX);
            int chunkY = ExpoShared.posToChunk(targetY);

            if(e.getChunkGrid().isActiveChunk(chunkX, chunkY)) {
                e.posX = targetX;
                e.posY = targetY;
                ServerPackets.p13EntityMove(e.entityId, e.velToPos(dir.x), e.velToPos(dir.y), e.posX, e.posY, Math.abs(dir.x) + Math.abs(dir.y), PacketReceiver.whoCanSee(e));
            } else {
                box.teleport(oldPosX, oldPosY);
            }
        } else {
            float dstX = e.posX + dir.x * delta * speed;
            float dstY = e.posY + dir.y * delta * speed;

            // Check for loaded chunk
            int chunkX = ExpoShared.posToChunk(dstX);
            int chunkY = ExpoShared.posToChunk(dstY);

            if(e.getChunkGrid().isActiveChunk(chunkX, chunkY)) {
                e.posX = dstX;
                e.posY = dstY;
                ServerPackets.p13EntityMove(e.entityId, e.velToPos(dir.x), e.velToPos(dir.y), e.posX, e.posY, Math.abs(dir.x) + Math.abs(dir.y), PacketReceiver.whoCanSee(e));
            }
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
