package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.util.EntityMetadata;
import dev.michey.expo.server.util.GenerationUtils;

public class BrainModuleStroll extends BrainModule {

    private float minDuration, maxDuration, strollSpeed;
    private float remainingDuration;
    private final Vector2 dirVector;

    public BrainModuleStroll() {
        this.dirVector = new Vector2();
    }

    @Override
    public void init() {
        EntityMetadata meta = getBrain().getEntity().getMetadata();

        minDuration = meta.getFloat("ai.strollMin");
        maxDuration = meta.getFloat("ai.strollMax");
        strollSpeed = meta.getFloat("ai.strollSpeed");
    }

    @Override
    public void onStart() {
        remainingDuration = MathUtils.random(minDuration, maxDuration);

        ServerEntity se = getBrain().getEntity();
        dirVector.set(GenerationUtils.circularRandom(1.0f).add(se.posX, se.posY)).sub(se.posX, se.posY).nor();
        getBrain().setLastMovementDirection(dirVector);
    }

    @Override
    public void onEnd() {

    }

    @Override
    public void tick(float delta) {
        remainingDuration -= delta;

        if(remainingDuration <= 0) {
            getBrain().setActiveModuleIfExisting(AIConstants.IDLE);
            return;
        }

        ServerEntity entity = getBrain().getEntity();
        float sp = entity.movementSpeedMultiplicator();
        entity.attemptMove(
                dirVector.x * delta * strollSpeed * sp,
                dirVector.y * delta * strollSpeed * sp
        );
    }

    @Override
    public int getType() {
        return AIConstants.STROLL;
    }

}
