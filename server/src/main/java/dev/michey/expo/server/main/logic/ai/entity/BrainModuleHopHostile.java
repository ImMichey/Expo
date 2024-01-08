package dev.michey.expo.server.main.logic.ai.entity;

import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.EntityMetadata;

import java.util.List;

import static dev.michey.expo.server.main.logic.ai.entity.BrainModuleIdleHostile.SEEK_PLAYER;

public class BrainModuleHopHostile extends BrainModuleHop {

    private List<ServerEntityType> seek;
    private float seekDistance;

    private float seekDelta;

    @Override
    public void init() {
        super.init();

        EntityMetadata meta = getBrain().getEntity().getMetadata();
        seek = meta.getEntityTypes("ai.seekTargets", SEEK_PLAYER);
        seekDistance = meta.getFloat("ai.hopSeekDistance");
    }

    @Override
    public void onStart() {
        super.onStart();
        seekDelta = 0.25f;
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        BrainModule chase = getBrain().hasModule(AIConstants.CHASE);
        if(chase == null) return;

        seekDelta -= delta;

        if(seekDelta <= 0) {
            // Do a search.
            ServerEntity find = findTarget(seek, seekDistance);

            if(find != null) {
                if(remainingDuration > 0) {
                    dirVector.set(find.posX, find.posY).sub(getBrain().getEntity().posX, getBrain().getEntity().posY).nor();
                    getBrain().setLastMovementDirection(dirVector);
                    return;
                }

                seekDelta = 0.25f;
                ((BrainModuleChase) chase).setMetadata(find);
                getBrain().setActiveModule(chase);
            } else {
                seekDelta = 0.25f;
            }
        }
    }

}