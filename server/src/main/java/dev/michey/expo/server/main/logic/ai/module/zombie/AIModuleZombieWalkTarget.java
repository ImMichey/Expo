package dev.michey.expo.server.main.logic.ai.module.zombie;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.main.logic.ai.module.AIModule;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;

public class AIModuleZombieWalkTarget extends AIModule {

    public int targetEntityId;
    private float speed;
    private float leashRange;

    public AIModuleZombieWalkTarget(AIState state, float minDuration, float maxDuration, float speed, float leashRange) {
        super(state, minDuration, maxDuration);
        this.speed = speed;
        this.leashRange = leashRange;
    }

    @Override
    public void tickModule(float delta) {
        var e = getBrain().getEntity();
        ServerEntity target = getBrain().getEntity().getDimension().getEntityManager().getEntityById(targetEntityId);

        if(target == null || target.health <= 0) {
            // Break.
            targetEntityId = -1;
            switchToStateIfPresent(AIState.IDLE);
            return;
        }

        float x = getBrain().getEntity().posX;
        float y = getBrain().getEntity().posY;
        float dst = Vector2.dst(target.posX, target.posY, x, y);

        if(dst > leashRange) {
            // Out of range.
            targetEntityId = -1;
            switchToStateIfPresent(AIState.IDLE);
            return;
        }

        float movementMultiplicator = e.movementSpeedMultiplicator();
        PhysicsEntity pe = (PhysicsEntity) e;
        EntityPhysicsBox box = pe.getPhysicsBox();

        Vector2 dir = new Vector2(target.posX, target.posY).sub(x, y).nor();
        float toMoveX = dir.x * delta * speed * movementMultiplicator;
        float toMoveY = dir.y * delta * speed * movementMultiplicator;
        var result = box.move(toMoveX, toMoveY, PhysicsBoxFilters.generalFilter);

        float targetX = result.goalX - box.xOffset;
        float targetY = result.goalY - box.yOffset;
        e.posX = targetX;
        e.posY = targetY;
        ServerPackets.p13EntityMove(e.entityId, e.velToPos(dir.x), e.velToPos(dir.y), e.posX, e.posY, PacketReceiver.whoCanSee(e));
    }

    @Override
    public void onEnd() {

    }

    @Override
    public void onStart() {

    }

}
