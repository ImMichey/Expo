package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.EntityMetadata;

public class BrainModuleHop extends BrainModule {

    private float hopDuration, hopSpeed, hopDelay;
    private int hopMin, hopMax;

    private int remainingHops;
    private float remainingDelay;
    float remainingDuration;
    final Vector2 dirVector;
    private boolean resetBetweenHop;

    public BrainModuleHop() {
        this.dirVector = new Vector2();
    }

    @Override
    public void init() {
        EntityMetadata meta = getBrain().getEntity().getMetadata();

        hopDuration = meta.getFloat("ai.hopDuration");
        hopSpeed = meta.getFloat("ai.hopSpeed");
        hopDelay = meta.getFloat("ai.hopDelay");
        hopMin = meta.getInt("ai.hopMin");
        hopMax = meta.getInt("ai.hopMax");
    }

    @Override
    public void onStart() {
        remainingHops = MathUtils.random(hopMin, hopMax);
        remainingDuration = 0;
        remainingDelay = 0;
        setRandomDirectionVector(dirVector);
    }

    @Override
    public void onEnd() {

    }

    @Override
    public void tick(float delta) {
        if(remainingDuration > 0) {
            // Hop.
            remainingDuration -= delta;

            ServerEntity entity = getBrain().getEntity();
            float sp = entity.movementSpeedMultiplicator();

            float mvx = dirVector.x * delta * hopSpeed * sp;
            float mvy = dirVector.y * delta * hopSpeed * sp;

            entity.attemptMove(
                    mvx,
                    mvy,
                    PhysicsBoxFilters.generalFilter, entity.velToPos(mvx), entity.velToPos(mvy)
            );
        } else {
            if(resetBetweenHop) {
                resetBetweenHop = false;
                getBrain().resetMovementPacket();
            }

            remainingDelay -= delta;

            if(remainingDelay <= 0) {
                remainingDelay = hopDelay;
                remainingHops--;
                remainingDuration = hopDuration;
                resetBetweenHop = true;
            } else if(remainingHops == 0) {
                getBrain().setActiveModuleIfExisting(AIConstants.IDLE);
            }
        }
    }

    @Override
    public int getType() {
        return AIConstants.STROLL;
    }

}
