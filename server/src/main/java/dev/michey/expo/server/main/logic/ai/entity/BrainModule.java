package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

import java.util.List;

public abstract class BrainModule {

    private EntityBrain brain;

    public void setBrain(EntityBrain brain) {
        this.brain = brain;
    }

    public abstract void init();
    public abstract void onStart();
    public abstract void onEnd();
    public abstract void tick(float delta);
    public abstract int getType();

    public EntityBrain getBrain() {
        return brain;
    }

    public ServerEntity findTarget(List<ServerEntityType> seek, float maxDistance) {
        ServerEntity e = getBrain().getEntity();
        float x = e.posX;
        float y = e.posY;

        float closestDst = maxDistance;
        ServerEntity closestEntity = null;

        for(ServerEntityType seekType : seek) {
            for(ServerEntity entity : e.getDimension().getEntityManager().getEntitiesOf(seekType)) {
                if(entity.health <= 0) continue;
                if(entity.invincibility > 0) continue;

                float dst = Vector2.dst(entity.posX, entity.posY, x, y);

                if(dst < closestDst) {
                    closestDst = dst;
                    closestEntity = entity;
                }
            }
        }

        return closestEntity;
    }

}