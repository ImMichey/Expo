package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.EntityMetadata;

public class BrainModuleChase extends BrainModule {

    private float chaseSpeed;
    private float maxChaseDistance;
    private float attackDistance;
    private float attackCooldown;
    private int chaseEntityId = -1;
    private final Vector2 dirVector;
    private float attackDelta;

    public BrainModuleChase() {
        this.dirVector = new Vector2();
    }

    @Override
    public void init() {
        EntityMetadata meta = getBrain().getEntity().getMetadata();

        maxChaseDistance = meta.getFloat("ai.chaseDistance");
        chaseSpeed = meta.getFloat("ai.chaseSpeed");
        attackDistance = meta.getFloat("ai.attackDistance");
        attackCooldown = meta.getFloat("ai.attackCooldown");

        attackDelta = attackCooldown;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onEnd() {

    }

    @Override
    public void tick(float delta) {
        ServerEntity entity = getBrain().getEntity();
        ServerEntity chase = entity.getDimension().getEntityManager().getEntityById(chaseEntityId);

        if(chase == null) {
            getBrain().resetModule();
            return;
        }

        float dst = Vector2.dst(chase.posX, chase.posY, entity.posX, entity.posY);

        if(dst > maxChaseDistance) {
            getBrain().resetModule();
        } else {
            boolean attack = false;
            BrainModule attackModule = getBrain().hasModule(AIConstants.ATTACK);

            if(attackModule != null) {
                attackDelta += delta;

                if(attackDelta > attackCooldown && dst <= attackDistance) {
                    attackDelta = 0;
                    attack = true;
                }
            }

            if(attack) {
                ((BrainModuleAttack) attackModule).setMetadata(chase);
                getBrain().setActiveModule(attackModule);
            } else if(dst > attackDistance) {
                if(entity.knockbackCalculations == null || entity.knockbackCalculations.isEmpty()) {
                    dirVector.set(chase.posX, chase.posY).sub(entity.posX, entity.posY).nor();
                    getBrain().setLastMovementDirection(dirVector);
                    float sp = entity.movementSpeedMultiplicator();

                    float mvx = dirVector.x * delta * chaseSpeed * sp;
                    float mvy = dirVector.y * delta * chaseSpeed * sp;

                    entity.attemptMove(
                            mvx,
                            mvy,
                            PhysicsBoxFilters.generalFilter, entity.velToPos(mvx), entity.velToPos(mvy)
                    );
                }
            } else {
                getBrain().resetMovementPacket();
            }
        }
    }

    @Override
    public int getType() {
        return AIConstants.CHASE;
    }

    public void setMetadata(ServerEntity chaseEntity) {
        chaseEntityId = chaseEntity.entityId;
    }

}
