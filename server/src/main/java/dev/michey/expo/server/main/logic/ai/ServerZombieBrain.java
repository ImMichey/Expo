package dev.michey.expo.server.main.logic.ai;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.hostile.ServerZombie;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.server.main.logic.ai.AIConstants.*;
import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class ServerZombieBrain {

    private final ServerZombie parent;
    public int currentMode = IDLE;
    private float modeDelta;
    private float duration;

    // target nearby players
    private int targetPlayerId = -1;
    private ServerPlayer targetPlayer = null;
    private float damageCooldown;
    private float scanCooldown = 1.0f;

    private final Vector2 dirVector = new Vector2();

    public ServerZombieBrain(ServerZombie parent) {
        this.parent = parent;
        generateDuration(3.0f, 8.0f);
    }

    public void tick(float delta) {
        if(damageCooldown > 0) {
            damageCooldown -= delta;
        }

        // ==== TARGET NEARBY PLAYER BLOCK ====
        if(targetPlayerId == -1) {
            if(scanCooldown > 0) {
                scanCooldown -= delta;

                if(scanCooldown <= 0) {
                    scanCooldown = 1.0f;
                    ServerPlayer found = findTargetPlayer();

                    if(found != null) {
                        targetPlayerId = found.entityId;
                        targetPlayer = found;
                    }
                }
            }
        } else {
            // Check
            ServerEntity existing = parent.getDimension().getEntityManager().getEntityById(targetPlayerId);

            if(existing == null) {
                targetPlayerId = -1;
                targetPlayer = null;
            } else {
                float dst = Vector2.dst(existing.posX, existing.posY, parent.posX, parent.posY);
                float MAX_LEASH_RANGE = 256;

                if(dst > MAX_LEASH_RANGE) {
                    // out of leash range
                    targetPlayerId = -1;
                    targetPlayer = null;
                } else {
                    float DAMAGE_RANGE = 16f;

                    if(dst <= DAMAGE_RANGE) {
                        if(damageCooldown <= 0) {
                            float preDamageHp = targetPlayer.health;
                            boolean applied = targetPlayer.applyDamageWithPacket(parent, 20.0f);

                            if(applied) {
                                // Damage.
                                damageCooldown = 1.0f;
                                ServerPackets.p24PositionalSound("slap", targetPlayer.posX, targetPlayer.posY, PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(targetPlayer));

                                // Apply knockback.
                                if(preDamageHp > targetPlayer.health) {
                                    targetPlayer.applyKnockback(ExpoShared.PLAYER_DEFAULT_ATTACK_KNOCKBACK_STRENGTH, ExpoShared.PLAYER_DEFAULT_ATTACK_KNOCKBACK_DURATION,
                                            new Vector2(targetPlayer.posX, targetPlayer.posY).sub(parent.posX, parent.posY).nor());
                                }

                                ServerPackets.p23PlayerLifeUpdate(targetPlayer.health, targetPlayer.hunger, PacketReceiver.player(targetPlayer));
                            }
                        }
                    }
                }
            }
        }

        if(targetPlayer != null) {
            // walk towards
            currentMode = CHASE;
            dirVector.set(targetPlayer.posX, targetPlayer.posY).sub(parent.posX, parent.posY).nor();
            float CHASE_SPEED = 24.0f;

            walk(delta, CHASE_SPEED);
        } else {
            if(currentMode == CHASE) {
                currentMode = IDLE;
                resetPacket();
            }

            modeDelta += delta;

            if(modeDelta >= duration) {
                if(currentMode == IDLE) {
                    currentMode = STROLL;
                    dirVector.set(GenerationUtils.circularRandom(1).add(parent.posX, parent.posY)).sub(parent.posX, parent.posY).nor();
                    generateDuration(4, 8);
                } else if(currentMode == STROLL) {
                    currentMode = IDLE;
                    generateDuration(3, 8);
                    resetPacket();
                }

                modeDelta = 0;
            } else {
                if(currentMode == STROLL) {
                    walk(delta, 16.0f);
                }
            }
        }
    }

    public void walk(float delta, float speed) {
        float movementMultiplicator = parent.movementSpeedMultiplicator();
        EntityPhysicsBox box = parent.getPhysicsBox();

        float toMoveX = dirVector.x * delta * speed * movementMultiplicator;
        float toMoveY = dirVector.y * delta * speed * movementMultiplicator;
        float oldPosX = parent.posX;
        float oldPosY = parent.posY;
        var result = box.move(toMoveX, toMoveY, PhysicsBoxFilters.generalFilter);

        float targetX = result.goalX - box.xOffset;
        float targetY = result.goalY - box.yOffset;

        // Check for loaded chunk
        int chunkX = ExpoShared.posToChunk(targetX);
        int chunkY = ExpoShared.posToChunk(targetY);

        if(parent.getChunkGrid().isActiveChunk(chunkX, chunkY)) {
            parent.posX = targetX;
            parent.posY = targetY;
            packet();
        } else {
            box.teleport(oldPosX, oldPosY);
        }
    }

    public void packet() {
        ServerPackets.p13EntityMove(parent.entityId,
                parent.velToPos(dirVector.x),
                parent.velToPos(dirVector.y),
                parent.posX,
                parent.posY,
                Math.abs(dirVector.x) + Math.abs(dirVector.y),
                PacketReceiver.whoCanSee(parent));
    }

    public void resetPacket() {
        ServerPackets.p13EntityMove(parent.entityId,
                parent.velToPos(0),
                parent.velToPos(0),
                parent.posX,
                parent.posY,
                0,
                PacketReceiver.whoCanSee(parent));
    }

    public void generateDuration(float min, float max) {
        duration = MathUtils.random(min, max);
    }

    private ServerPlayer findTargetPlayer() {
        float x = parent.posX;
        float y = parent.posY;

        for(ServerPlayer player : parent.getDimension().getEntityManager().getAllPlayers()) {
            if(player.health <= 0) continue;
            if(player.invincibility > 0) continue;

            float dst = Vector2.dst(player.posX, player.posY, x, y);
            float MAX_SEARCH_RANGE = 192;
            if(dst <= MAX_SEARCH_RANGE) return player;
        }

        return null;
    }

}