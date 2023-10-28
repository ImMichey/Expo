package dev.michey.expo.server.main.logic.ai.entity;

import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.EntityMetadata;

import java.util.LinkedList;
import java.util.List;

public class BrainModuleIdleHostile extends BrainModuleIdle {

    public static final LinkedList<ServerEntityType> SEEK_PLAYER;
    public static final LinkedList<ServerEntityType> SEEK_SMALL;

    static {
        SEEK_PLAYER = new LinkedList<>();
        SEEK_PLAYER.add(ServerEntityType.PLAYER);

        SEEK_SMALL = new LinkedList<>();
        SEEK_SMALL.add(ServerEntityType.WORM);
        SEEK_SMALL.add(ServerEntityType.MAGGOT);
    }

    private List<ServerEntityType> seek;
    private float seekDistance;

    private float seekDelta;

    @Override
    public void init() {
        super.init();

        EntityMetadata meta = getBrain().getEntity().getMetadata();
        seek = meta.getEntityTypes("ai.seekTargets");
        seekDistance = meta.getFloat("ai.idleSeekDistance");
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
