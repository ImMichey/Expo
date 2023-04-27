package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.server.main.logic.ai.module.AIModuleIdle;
import dev.michey.expo.server.main.logic.ai.module.AIModuleWalk;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.util.AIState;

public class ServerWorm extends ServerEntity {

    public EntityBrain wormBrain = new EntityBrain(this);

    public ServerWorm() {
        health = 20.0f;
    }

    @Override
    public void onCreation() {
        wormBrain.addModule(new AIModuleIdle(AIState.IDLE, 2.0f, 6.0f));
        wormBrain.addModule(new AIModuleWalk(AIState.WALK, 2.0f, 6.0f, 5.0f));
    }

    @Override
    public void tick(float delta) {
        wormBrain.tick(delta);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.WORM;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

}
