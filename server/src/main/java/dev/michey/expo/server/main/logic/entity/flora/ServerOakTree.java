package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.util.SpawnItem;
import org.json.JSONObject;

public class ServerOakTree extends ServerEntity {

    /** Physics body */
    private BoundingBox physicsBody;

    public int age;
    public int variant;

    public static final float[][] TREE_BODIES = new float[][] {
        new float[] {2.0f, 4.0f, 13.0f, 4.5f},
        new float[] {2.0f, 4.0f, 13.0f, 4.5f},
        new float[] {2.0f, 4.0f, 13.0f, 4.5f},
        new float[] {2.0f, 4.0f, 15.0f, 4.5f},
        new float[] {2.0f, 4.0f, 17.0f, 4.5f}
    };

    @Override
    public void onCreation() {
        // add physics body of player to world
        if(variant == 0) variant = 1;
        float[] b = TREE_BODIES[variant - 1];
        physicsBody = new BoundingBox(this, b[0], b[1], b[2], b[3]);
        damageableWith = ToolType.AXE;
    }

    @Override
    public void onDie() {
        int min = 3, max = 6;

        if(age == 1) {
            min += 1;
            max += 1;
        } else if(age == 2) {
            min += 2;
            max += 3;
        }

        spawnEntitiesAround(8.5f, 6.0f, 14.0f, 18.0f,
                new SpawnItem("item_oak_log", min, max),
                new SpawnItem("item_acorn", 1, 2)
        );
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        generateAge(biome);
        generateVariant();
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        return super.onDamage(damageSource, damage);
    }

    @Override
    public void onDeletion() {
        // remove physics body of player from world
        physicsBody.dispose();
    }

    public ServerOakTree() {
        health = 50.0f;
        damageableWith = ToolType.AXE;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.OAK_TREE;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("variant", variant);
    }

    @Override
    public void onLoad(JSONObject saved) {
        variant = saved.getInt("variant");
        ageFromVariant();
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant};
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
            } else if(r <= 80) {
                age = 1;
            } else {
                age = 2;
            }
        }
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

}
