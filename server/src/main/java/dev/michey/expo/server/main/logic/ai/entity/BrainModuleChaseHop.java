package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.EntityMetadata;

public class BrainModuleChaseHop extends BrainModuleChase {

    private float chaseHopDuration, chaseHopDelay;

    private float remainingDelay;
    private float remainingDuration;
    private boolean resetBetweenHop;

    @Override
    public void init() {
        super.init();
        EntityMetadata meta = getBrain().getEntity().getMetadata();

        chaseHopDuration = meta.getFloat("ai.chaseHopDuration");
        chaseHopDelay = meta.getFloat("ai.chaseHopDelay");
    }

    @Override
    public void onStart() {
        remainingDuration = 0;
        remainingDelay = 0;
    }

    @Override
    public void tick(float delta) {
        ServerEntity entity = getBrain().getEntity();
        ServerEntity chase = entity.getDimension().getEntityManager().getEntityById(chaseEntityId);

        if(chase == null) {
            getBrain().resetModule();
            return;
        }

        float dst = Vector2.dst(chase.posX, chase.posY, entity.posX, entity.posY + attackOffsetY);

        if(dst > maxChaseDistance && remainingDuration <= 0) {
            getBrain().resetModule();
        } else {
            boolean attack = false;
            BrainModule attackModule = getBrain().hasModule(AIConstants.ATTACK);

            if(attackModule != null) {
                attackDelta += delta;

                if(attackDelta > attackCooldown && dst <= attackDistance) {
                    if(remainingDuration > 0) {
                        remainingDuration -= delta;
                    } else {
                        getBrain().resetMovementPacket();
                        attackDelta = 0;
                        attack = true;
                    }
                }
            }

            if(attack) {
                ((BrainModuleAttack) attackModule).setMetadata(chase);
                getBrain().setActiveModule(attackModule);
            } else if(dst > attackDistance) {
                if(entity.knockbackCalculations == null || entity.knockbackCalculations.isEmpty()) {
                    if(remainingDuration > 0) {
                        // Hop.
                        remainingDuration -= delta;

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
                    } else {
                        if(resetBetweenHop) {
                            resetBetweenHop = false;
                            getBrain().resetMovementPacket();
                        }

                        remainingDelay -= delta;

                        if(remainingDelay <= 0) {
                            remainingDelay = chaseHopDelay;
                            remainingDuration = chaseHopDuration;
                            resetBetweenHop = true;
                        }
                    }
                }
            }
        }
    }

}