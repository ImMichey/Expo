package dev.michey.expo.server.main.logic.ai;

import dev.michey.expo.server.main.logic.ai.module.AIModule;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.util.AIState;

import java.util.HashMap;

public class EntityBrain {

    private final ServerEntity parent;
    private final HashMap<AIState, AIModule> aiModules;
    private AIState currentState;
    private AIModule currentModule;
    private float currentDelta;

    public EntityBrain(ServerEntity parent) {
        this.parent = parent;
        aiModules = new HashMap<>();
    }

    public void addModule(AIModule module) {
        module.setBrain(this);
        aiModules.put(module.getState(), module);

        if(aiModules.size() == 1) {
            // First entry so set this as active
            setActiveModule(module.getState());
        }
    }

    public void tick(float delta) {
        currentDelta += delta;

        if(currentDelta >= currentModule.getDuration()) {
            currentModule.onEnd();
            currentDelta = 0;
        } else {
            currentModule.tickModule(delta);
        }
    }

    public void setAnyActiveModule(AIState... states) {
        for(AIState state : states) {
            AIModule existing = aiModules.get(state);

            if(existing != null) {
                currentState = state;
                currentModule = existing;
                currentModule.generateDuration();
                currentModule.onStart();
                return;
            }
        }

        // Restart current module if none found
        currentModule.generateDuration();
        currentModule.onStart();
    }

    public void setActiveModule(AIState state) {
        AIModule existing = aiModules.get(state);

        if(existing != null) {
            currentState = state;
            currentModule = existing;
            currentModule.generateDuration();
            currentModule.onStart();
        }
    }

    public AIState getCurrentState() {
        return currentState;
    }

    public AIModule getModule(AIState state) {
        return aiModules.get(state);
    }

    public ServerEntity getEntity() {
        return parent;
    }

}
