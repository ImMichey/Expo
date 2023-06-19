package dev.michey.expo.server.main.logic.ai.module;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.util.AIState;

public abstract class AIModule {

    private EntityBrain brain;
    private final AIState state;
    private final float minDuration;
    private final float maxDuration;
    private float duration;

    public AIModule(AIState state, float minDuration, float maxDuration) {
        this.state = state;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    public abstract void tickModule(float delta);
    public abstract void onEnd();
    public abstract void onStart();

    public void generateDuration() {
        duration = MathUtils.random(minDuration, maxDuration);
    }

    public float getDuration() {
        return duration;
    }

    public AIState getState() {
        return state;
    }

    public void setBrain(EntityBrain brain) {
        this.brain = brain;
    }

    public EntityBrain getBrain() {
        return brain;
    }

    public void switchToStateIfPresent(AIState... states) {
        brain.setAnyActiveModule(states);
    }

}
