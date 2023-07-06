package dev.michey.expo.server.main.logic.ai.module.zombie;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.module.AIModule;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.AIState;

import java.util.LinkedList;
import java.util.List;

public class AIModuleZombieFindTarget extends AIModule {

    private final float iterationCooldown;
    private final float maxRange;
    private final List<ServerEntityType> targets;
    private float cooldownDelta;
    private float idleDelta;

    private Vector2 idleWalkDestination;

    public static final LinkedList<ServerEntityType> DEFAULT_TARGETS;

    static {
        DEFAULT_TARGETS = new LinkedList<>();
        DEFAULT_TARGETS.add(ServerEntityType.PLAYER);
    }

    public AIModuleZombieFindTarget(AIState state, float minDuration, float maxDuration, float iterationCooldown, float maxRange, List<ServerEntityType> targets) {
        super(state, minDuration, maxDuration);
        this.iterationCooldown = iterationCooldown;
        this.maxRange = maxRange;
        this.targets = targets;
        cooldownDelta = 0.5f;
    }

    @Override
    public void tickModule(float delta) {
        cooldownDelta -= delta;
        boolean found = false;

        if(cooldownDelta <= 0) {
            cooldownDelta = iterationCooldown;
            found = find();
        }

        if(!found) {
            idleDelta -= delta;

            if(idleDelta <= 0) {
                idleDelta = MathUtils.random(3.0f, 8.0f);
                idleWalkDestination = GenerationUtils.circularRandom(1.0f).nor();
            }
        }
    }

    private boolean find() {
        float x = getBrain().getEntity().posX;
        float y = getBrain().getEntity().posY;
        ServerEntity useAsTarget = null;

        skipAll: for(ServerEntityType type : targets) {
            List<ServerEntity> possibleTargets = getBrain().getEntity().getDimension().getEntityManager().getEntitiesOf(type);

            for(ServerEntity e : possibleTargets) {
                float dst = Vector2.dst(e.posX, e.posY, x, y);

                if(dst <= maxRange) {
                    // Found
                    useAsTarget = e;
                    break skipAll;
                }
            }
        }

        if(useAsTarget != null) {
            AIModuleZombieWalkTarget walk = (AIModuleZombieWalkTarget) getBrain().getModule(AIState.WALK);
            walk.targetEntityId = useAsTarget.entityId;
            switchToStateIfPresent(AIState.WALK);
            return true;
        }

        return false;
    }

    @Override
    public void onEnd() {

    }

    @Override
    public void onStart() {
        var e = getBrain().getEntity();
        ServerPackets.p13EntityMove(e.entityId, 0, 0, e.posX, e.posY, PacketReceiver.whoCanSee(e));
    }

}
