package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
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
import org.json.JSONObject;

public class ServerOakTree extends ServerEntity implements PhysicsEntity {

    /** Physics body */
    private EntityPhysicsBox physicsBody;

    public int age;
    public int variant;
    public boolean cut;
    public boolean emptyCrown;
    public float trunkConversionHealth = 30;
    public float leavesOffset;

    public boolean falling;
    public float fallingEnd;
    public boolean fallingDirectionRight;
    public static float FALLING_ANIMATION_DURATION = 4.25f;

    public static final float[][] TREE_BODIES = new float[][] {
        new float[] {-6.0f, 4.0f, 13.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 13.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 13.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 15.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 17.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 15.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 15.0f, 4.5f},
    };
    public static final float[] TREE_HEALTH = new float[] {
            120.0f,
            150.0f,
            180.0f,
    };

    @Override
    public void onCreation() {
        // add physics body of player to world
        if(variant == 0) variant = 1;
        float[] b = TREE_BODIES[variant - 1];
        physicsBody = new EntityPhysicsBox(this, b[0], b[1], b[2], b[3]);
        setDamageableWith(ToolType.AXE);
    }

    @Override
    public void onDie() {
        spawnItemsAround(0.0f, 6.0f, 8, 8, new SpawnItem("item_oak_log", 1, 1));
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        generateAge(biome, rnd);
        generateVariant(rnd);
        leavesOffset = -rnd.random(0, 10);

        if(rnd.random() <= 0.03f) {
            cut = true;
            health = trunkConversionHealth;
        }
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
                ServerPackets.p30EntityDataUpdate(entityId, new Object[] {cut, falling, fallingEnd, fallingDirectionRight}, PacketReceiver.whoCanSee(this));

                int reach = 90;
                int minLog = 2;
                int maxLog = 5;

                if(variant == 1 || variant == 2) {
                    reach = 70;
                    maxLog -= 1;
                } else if(variant == 4) {
                    reach = 120;
                } else if(variant == 5) {
                    reach = 220;
                    minLog += 2;
                    maxLog += 2;
                } else if(variant == 6) {
                    reach = 195;
                    minLog += 1;
                    maxLog += 1;
                }
                float factor = fallingDirectionRight ? 1 : -1;

                spawnItemsAlongLine(posX + ((20 + leavesOffset) * factor), posY, reach * factor, 0, 8.0f,
                        new SpawnItem("item_oak_log", minLog, maxLog),
                        new SpawnItem("item_acorn", 1, 2));

                float _x1 = posX + (20 + leavesOffset) * factor;
                float _x2 = posX + ((20 + leavesOffset) + reach) * factor;
                float x1 = factor == 1 ? _x1 : _x2;
                float x2 = factor == 1 ? _x2 : _x1;

                float[] damageAreaVertices = new float[] {
                        x1,
                        posY,
                        x2,
                        posY + 10f
                };
                applyDamageToArea(damageAreaVertices, 40, 32.0f, 0.25f, true, false);
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
                .add("variant", variant)
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
        variant = saved.getInt("variant");
        cut = saved.getBoolean("cut");
        leavesOffset = saved.getFloat("leavesOffset");
        if(saved.has("falling")) {
            falling = true;
            fallingEnd = saved.getFloat("fallingEnd");
            fallingDirectionRight = saved.getBoolean("fallingDirectionRight");
        }
        emptyCrown = saved.getBoolean("emptyCrown");
        ageFromVariant();
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant, cut, falling, fallingEnd, fallingDirectionRight, leavesOffset};
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
            ServerPackets.p30EntityDataUpdate(entityId, new Object[] {cut, falling, fallingEnd, fallingDirectionRight}, PacketReceiver.whoCanSee(this));
        }

        return true;
    }

    public void generateAge(BiomeType biome, GenerationRandom rnd) {
        int r = rnd == null ? MathUtils.random(100) : rnd.random(100);

        if(r <= 75) {
            age = 0;
        } else if(r <= 95) {
            age = 1;
        } else {
            age = 2;
        }

        health = TREE_HEALTH[age];
    }

    public void generateVariant(GenerationRandom rnd) {
        if(age == 0) {
            variant = rnd == null ? MathUtils.random(1, 2) : rnd.random(1, 2);
        } else if(age == 1) {
            //variant = rnd == null ? MathUtils.random(3, 4) : rnd.random(3, 4);
            variant = 4;
        } else if(age == 2) {
            variant = rnd == null ? MathUtils.random(5, 6) : rnd.random(5, 6);
        }
    }

    public void ageFromVariant() {
        if(variant <= 2) {
            age = 0;
        } else if(variant == 3 || variant == 4) {
            age = 1;
        } else if(variant == 5 || variant == 6) {
            age = 2;
        }
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public void onMoved() {

    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.HEAVY;
    }

}
