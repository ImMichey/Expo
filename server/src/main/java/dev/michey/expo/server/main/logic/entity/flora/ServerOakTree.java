package dev.michey.expo.server.main.logic.entity.flora;

import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.server.util.SpawnItem;
import dev.michey.expo.util.EntityRemovalReason;
import org.json.JSONObject;

public class ServerOakTree extends ServerEntity implements PhysicsEntity {

    /** Physics body */
    private EntityPhysicsBox physicsBody;

    public int trunkVariant;

    public boolean cut;
    public boolean emptyCrown;

    public float trunkConversionHealth = 30;
    public float leavesOffset;

    public boolean falling;
    public float fallingEnd;
    public boolean fallingDirectionRight;
    public static float FALLING_ANIMATION_DURATION = 4.25f;

    public static final float[][] TREE_BODIES = new float[][] {
        new float[] {-7.0f, 4.0f, 14.0f, 4.5f},
        new float[] {-7.0f, 4.0f, 14.0f, 4.5f},
        new float[] {-7.0f, 4.0f, 14.0f, 4.5f},
        new float[] {-7.0f, 4.0f, 14.0f, 4.5f},
        new float[] {-7.0f, 4.0f, 14.0f, 4.5f},
        new float[] {-7.0f, 4.0f, 14.0f, 4.5f},
    };
    public static final float[] TREE_HEALTH = new float[] {
            120.0f,
            150.0f,
            120.0f,
            150.0f,
            120.0f,
            150.0f,
    };

    @Override
    public void onCreation() {
        // add physics body of player to world
        if(trunkVariant == 0) {
            trunkVariant = 1;
        }
        float[] b = TREE_BODIES[trunkVariant - 1];
        physicsBody = new EntityPhysicsBox(this, b[0], b[1], b[2], b[3]);
        setDamageableWith(ToolType.AXE);
    }

    @Override
    public void onDie() {
        spawnItemsAround(0.0f, 6.0f, 8, 8, new SpawnItem("item_oak_log", 1, 1));
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        trunkVariant = rnd.random(1, 6);

        leavesOffset = -rnd.random(0f, 14f);

        float modifyRandom = rnd.random();

        if(modifyRandom <= 0.03f) {
            cut = true;
            health = trunkConversionHealth;
        }/* else if(modifyRandom <= 0.06f) {
            emptyCrown = true;
        }
        */
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();
    }

    public ServerOakTree() {
        health = TREE_HEALTH[0];
        setDamageableWith(ToolType.AXE);
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;

        if(falling) {
            fallingEnd -= delta;

            if(fallingEnd <= 0) {
                falling = false;
                ServerPackets.p30EntityDataUpdate(entityId, getPacketPayload(), PacketReceiver.whoCanSee(this));

                float th = 0;
                int minLog = 3;
                int maxLog = 5;

                if(emptyCrown) {
                    if(trunkVariant % 2 == 0) {
                        th = 47;
                    } else if(trunkVariant % 2 == 1) {
                        th = 66;
                    }
                    minLog -= 1;
                    maxLog -= 2;
                } else {
                    if(trunkVariant % 2 == 0) {
                        th = 109;
                    } else if(trunkVariant % 2 == 1) {
                        th = 128;
                    }
                }
                float reach = th - 15 + leavesOffset;

                float factor = fallingDirectionRight ? 1 : -1;

                float _x1 = posX;
                float _x2 = posX + reach * factor;
                float x1 = factor == 1 ? _x1 : _x2;
                float x2 = factor == 1 ? _x2 : _x1;

                float[] damageAreaVertices = new float[] {
                        x1,
                        posY,
                        x2,
                        posY + 15f
                };
                applyDamageToArea(damageAreaVertices, 40, 32.0f, 0.25f, true, false);

                float spawnItemsX;
                float spawnItemsY = posY + 7.5f;
                float reachItemsX;

                if(factor == 1) {
                    // Right side.
                    spawnItemsX = posX + 12.5f;
                    reachItemsX = th - 12.5f - 8f;
                } else {
                    // Left side.
                    spawnItemsX = posX - th + 8f;
                    reachItemsX = th - 12.5f - 8f;
                }

                spawnItemsAlongLine(spawnItemsX, spawnItemsY, reachItemsX, 0, 8.0f,
                        new SpawnItem("item_oak_log", minLog, maxLog),
                        new SpawnItem("item_acorn", 1, 2));
            }
        }
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.OAK_TREE;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack()
                .add("tv", trunkVariant)
                .add("cut", cut)
                .add("leavesOffset", leavesOffset)
                .optional("falling", falling, falling)
                .optional("fallingEnd", fallingEnd, falling)
                .optional("fallingDirectionRight", fallingDirectionRight, falling)
                .add("emptyCrown", emptyCrown)
                ;
    }

    @Override
    public void onLoad(JSONObject saved) {
        trunkVariant = saved.getInt("tv");
        cut = saved.getBoolean("cut");
        leavesOffset = saved.getFloat("leavesOffset");
        if(saved.has("falling")) {
            falling = true;
            fallingEnd = saved.getFloat("fallingEnd");
            fallingDirectionRight = saved.getBoolean("fallingDirectionRight");
        }
        emptyCrown = saved.getBoolean("emptyCrown");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {new TreeData(trunkVariant, cut, emptyCrown, leavesOffset, falling, fallingDirectionRight, fallingEnd)};
    }

    @Override
    public boolean applyDamageWithPacket(ServerEntity damageSource, float damage) {
        boolean currentCut = cut;
        boolean applied = onDamage(damageSource, damage);

        if(applied) {
            health -= damage;

            if(!currentCut && cut && health <= 0) {
                health = 1;
            }

            boolean damagePacket = true;

            if(health <= 0) {
                damagePacket = getEntityType() == ServerEntityType.PLAYER;
                killEntityWithAdvancedPacket(damage, health, damageSource.entityId, EntityRemovalReason.DEATH);
            }

            if(damagePacket) {
                ServerPackets.p26EntityDamage(entityId, damage, health, damageSource.entityId, PacketReceiver.whoCanSee(this));
            }
        }

        return applied;
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        if(invincibility > 0) return false;
        float newHp = health - damage;

        if(newHp <= trunkConversionHealth && !cut) {
            cut = true;
            falling = true;
            fallingEnd = FALLING_ANIMATION_DURATION;
            invincibility = FALLING_ANIMATION_DURATION;

            fallingDirectionRight = damageSource.posX < posX;
            ServerPackets.p30EntityDataUpdate(entityId, getPacketPayload(), PacketReceiver.whoCanSee(this));
        }

        return true;
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.HEAVY;
    }

    public record TreeData(int trunkVariant, boolean cut, boolean emptyCrown, float leavesOffset, boolean falling, boolean fallingDirectionRight, float fallingRemaining) {

    }

}
