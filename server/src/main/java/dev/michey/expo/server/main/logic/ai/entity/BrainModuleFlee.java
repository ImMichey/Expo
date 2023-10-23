package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.util.EntityMetadata;

public class BrainModuleFlee extends BrainModule {

    private float minDuration, maxDuration, fleeSpeed;
    private float remainingDuration;
    private Vector2 dirVector;

    private int attackerId;
    private float attackDamage;

    public BrainModuleFlee() {
        this.dirVector = new Vector2();
    }

    @Override
    public void init() {
        EntityMetadata meta = getBrain().getEntity().getMetadata();

        minDuration = meta.getFloat("ai.fleeMin");
        maxDuration = meta.getFloat("ai.fleeMax");
        fleeSpeed = meta.getFloat("ai.fleeSpeed");
    }

    @Override
    public int getType() {
        return AIConstants.FLEE;
    }

    @Override
    public void onStart() {
        remainingDuration = MathUtils.random(minDuration, maxDuration);
    }

    @Override
    public void onEnd() {

    }

    public void setMetadata(int attackerId, float attackDamage) {
        this.attackerId = attackerId;
        this.attackDamage = attackDamage;
    }

    @Override
    public void tick(float delta) {
        remainingDuration -= delta;

        if(remainingDuration <= 0) {
            getBrain().resetModule();
            return;
        }

        ServerEntity entity = getBrain().getEntity();
        ServerEntity attacker = entity.getDimension().getEntityManager().getEntityById(attackerId);

        if(attacker == null) {
            getBrain().resetModule();
            return;
        }

        // Flee vector
        dirVector.set(entity.posX, entity.posY).sub(attacker.posX, attacker.posY).nor();
        getBrain().setLastMovementDirection(dirVector);
        float sp = entity.movementSpeedMultiplicator();
        entity.attemptMove(
                dirVector.x * delta * fleeSpeed * sp,
                dirVector.y * delta * fleeSpeed * sp
        );
    }

}