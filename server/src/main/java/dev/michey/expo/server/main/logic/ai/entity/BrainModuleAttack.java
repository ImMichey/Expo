package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.util.EntityMetadata;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class BrainModuleAttack extends BrainModule {

    private float attackDuration;
    private float attackDelta;
    private float attackDamage;
    private float attackAt;
    private float attackRange;
    private float attackOffsetY;
    private int attackEntityId = -1;
    private boolean attackForce;

    public BrainModuleAttack() {

    }

    @Override
    public void init() {
        EntityMetadata meta = getBrain().getEntity().getMetadata();

        attackDuration = meta.getFloat("ai.attackDuration");
        attackDamage = meta.getFloat("ai.attackDamage");
        attackAt = meta.getFloat("ai.attackAt");
        attackRange = meta.getFloat("ai.attackRange");
        attackOffsetY = meta.getAttackOffsetY();
    }

    @Override
    public void onStart() {
        attackDelta = 0;
        attackForce = false;
        getBrain().resetMovementPacket();
        ServerPackets.p42EntityAnimation(getBrain().getEntity().entityId, 0, PacketReceiver.whoCanSee(getBrain().getEntity()));
    }

    @Override
    public void onEnd() {

    }

    @Override
    public void tick(float delta) {
        attackDelta += delta;

        if(attackDelta >= attackAt) {
            if(!attackForce) {
                attackForce = true;

                ServerEntity attackEntity = getBrain().getEntity().getDimension().getEntityManager().getEntityById(attackEntityId);

                if(attackEntity != null && Vector2.dst(attackEntity.posX, attackEntity.posY, getBrain().getEntity().posX, getBrain().getEntity().posY + attackOffsetY) <= attackRange) {
                    float preDamageHp = attackEntity.health;
                    boolean applied = attackEntity.applyDamageWithPacket(getBrain().getEntity(), attackDamage);

                    if(applied) {
                        ServerPackets.p24PositionalSound("slap", attackEntity.posX, attackEntity.posY, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(attackEntity));

                        // Apply knockback.
                        if(preDamageHp > attackEntity.health) {
                            attackEntity.addKnockback(ExpoShared.PLAYER_DEFAULT_ATTACK_KNOCKBACK_STRENGTH, ExpoShared.PLAYER_DEFAULT_ATTACK_KNOCKBACK_DURATION,
                                    new Vector2(attackEntity.posX, attackEntity.posY).sub(getBrain().getEntity().posX, getBrain().getEntity().posY).nor());
                        }

                        if(attackEntity instanceof ServerPlayer sp) {
                            ServerPackets.p23PlayerLifeUpdate(sp.health, sp.hunger, PacketReceiver.player(sp));
                        }
                    }
                }
            }
        }

        if(attackDelta >= attackDuration) {
            getBrain().resetMovementPacket();
            getBrain().setActiveModuleIfExisting(AIConstants.CHASE);
        }
    }

    @Override
    public int getType() {
        return AIConstants.ATTACK;
    }

    public void setMetadata(ServerEntity attack) {
        attackEntityId = attack.entityId;
    }

}
