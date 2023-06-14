package dev.michey.expo.server.main.logic.entity.animal;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.ai.EntityBrain;
import dev.michey.expo.server.main.logic.ai.module.AIModuleIdle;
import dev.michey.expo.server.main.logic.ai.module.AIModuleWalk;
import dev.michey.expo.server.main.logic.entity.arch.DamageableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitboxMapper;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;

public class ServerWorm extends ServerEntity implements DamageableEntity {

    public EntityBrain wormBrain = new EntityBrain(this);

    public ServerWorm() {
        health = 40.0f;
    }

    @Override
    public void onCreation() {
        wormBrain.addModule(new AIModuleIdle(AIState.IDLE, 2.0f, 6.0f));
        wormBrain.addModule(new AIModuleWalk(AIState.WALK, 2.0f, 6.0f, 5.0f));
    }

    @Override
    public void onDie() {
        spawnEntitiesAround(1, 1, 0, 0.25f, "item_worm", 8);
    }

    @Override
    public void tick(float delta) {
        tickKnockback(delta);
        wormBrain.tick(delta);

        if(wormBrain.getCurrentState() != AIState.WALK) {
            ServerPackets.p13EntityMove(entityId, 0, 0, posX, posY, PacketReceiver.whoCanSee(this));
        }
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.WORM;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack();
    }

    @Override
    public EntityHitbox getEntityHitbox() {
        return EntityHitboxMapper.get().getFor(ServerEntityType.WORM);
    }

}
