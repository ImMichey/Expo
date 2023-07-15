package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
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
    public float trunkConversionHealth;

    public boolean falling;
    public float fallingEnd;
    public boolean fallingDirectionRight;
    public static float FALLING_ANIMATION_DURATION = 4.25f;

    public static final float[][] TREE_BODIES = new float[][] {
        new float[] {-6.0f, 4.0f, 13.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 13.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 13.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 15.0f, 4.5f},
        new float[] {-6.0f, 4.0f, 17.0f, 4.5f}
    };
    public static final float[] TREE_HEALTH = new float[] {
            120.0f,
            160.0f,
            200.0f,
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
        spawnItemsAround(0.0f, 6.0f, 10f, 14f, new SpawnItem("item_oak_log", 1, 2));
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        generateAge(biome);
        generateVariant();

        if(MathUtils.random() <= 0.1f) {
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
        trunkConversionHealth = health * 0.5f;
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

                if(variant == 4) {
                    reach = 120;
                }

                spawnItemsAlongLine(posX + (20 * (fallingDirectionRight ? 1 : -1)), posY, reach * (fallingDirectionRight ? 1 : -1), 0, 10.0f,
                        new SpawnItem("item_oak_log", 2, 5),
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
                .add("variant", variant)
                .add("cut", cut)
                .optional("falling", falling, falling)
                .optional("fallingEnd", fallingEnd, falling)
                .optional("fallingDirectionRight", fallingDirectionRight, falling)
                ;
    }

    @Override
    public void onLoad(JSONObject saved) {
        variant = saved.getInt("variant");
        cut = saved.getBoolean("cut");
        if(saved.has("falling")) {
            falling = true;
            fallingEnd = saved.getFloat("fallingEnd");
            fallingDirectionRight = saved.getBoolean("fallingDirectionRight");
        }
        ageFromVariant();
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant, cut, falling, fallingEnd, fallingDirectionRight};
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

    public void generateAge(BiomeType biome) {
        if(biome == BiomeType.PLAINS) {
            if(MathUtils.random(1, 5) <= 4) {
                age = 0;
            } else {
                age = 1;
            }
        } else {
            int r = MathUtils.random(100);

            if(r <= 60) {
                age = 0;
            } else {
                age = 1;
                /* if(r <= 80) {
                age = 1;
            } else {
                age = 2;
            }*/
            }
        }

        health = TREE_HEALTH[age];
        trunkConversionHealth = health * 0.5f;
    }

    public void generateVariant() {
        if(age == 0) {
            variant = MathUtils.random(1, 3);
        } else if(age == 1) {
            variant = 4;
        } else if(age == 2) {
            variant = 5;
        }
    }

    public void ageFromVariant() {
        if(variant <= 3) {
            age = 0;
        } else if(variant == 4) {
            age = 1;
        } else if(variant == 5) {
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
