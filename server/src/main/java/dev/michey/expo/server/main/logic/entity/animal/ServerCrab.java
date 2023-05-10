package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.server.main.logic.ai.module.AIModuleIdle;
import dev.michey.expo.server.main.logic.ai.module.AIModuleWalk;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.util.AIState;

public class ServerCrab extends ServerEntity {

    public EntityBrain crabBrain = new EntityBrain(this);

    public ServerCrab() {
        health = 20.0f;
    }

    @Override
    public void onCreation() {
        crabBrain.addModule(new AIModuleIdle(AIState.IDLE, 2.0f, 6.0f));
        crabBrain.addModule(new AIModuleWalk(AIState.WALK, 2.0f, 6.0f, 16.0f));
    }

    @Override
    public void tick(float delta) {
        crabBrain.tick(delta);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CRAB;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
