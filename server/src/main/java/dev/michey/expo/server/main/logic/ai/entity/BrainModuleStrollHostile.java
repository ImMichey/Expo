package dev.michey.expo.server.main.logic.ai.entity;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.EntityMetadata;

import java.util.List;

public class BrainModuleStrollHostile extends BrainModuleStroll {

    private List<ServerEntityType> seek;
    private float seekDistance;

    private float seekDelta;

    @Override
    public void init() {
        super.init();

        EntityMetadata meta = getBrain().getEntity().getMetadata();
        seek = meta.getEntityTypes("ai.seekTargets");
        seekDistance = meta.getFloat("ai.strollSeekDistance");
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
            seekDelta = 0.25f;

            // Do a search.
            ServerEntity find = findTarget(seek, seekDistance);

            if(find != null) {
                ((BrainModuleChase) chase).setMetadata(find);
                getBrain().setActiveModule(chase);
            }
        }
    }

}
