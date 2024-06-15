package dev.michey.expo.server.main.logic.entity.misc;

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
import dev.michey.expo.server.util.SpawnItem;
import org.json.JSONObject;

public class ServerBoulder extends ServerEntity implements PhysicsEntity {

    /** Physics body */
    private EntityPhysicsBox physicsBody;

    public int variant = 1;

    public static final int VARIANT_IRON = 5;
    public static final int VARIANT_COAL = 3;
    public static final int VARIANT_REG = 1;

    public static final float[][] ROCK_BODIES = new float[][] {
            new float[] {-9.0f, 2.0f, 19.0f, 6.0f},
            new float[] {-9.0f, 2.0f, 19.0f, 6.0f},
            new float[] {-9.0f, 2.0f, 19.0f, 6.0f},
            new float[] {-9.0f, 2.0f, 19.0f, 6.0f},
            new float[] {-9.0f, 2.0f, 19.0f, 6.0f},
            new float[] {-9.0f, 2.0f, 19.0f, 6.0f},
    };

    public ServerBoulder() {
        setDamageableWith(ToolType.PICKAXE);
    }

    @Override
    public void onCreation() {
        // add physics body of player to world
        float[] b = ROCK_BODIES[variant - 1];
        physicsBody = new EntityPhysicsBox(this, b[0], b[1], b[2], b[3]);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        float ch = rnd.random();
        int add = (biome == BiomeType.DESERT || biome == BiomeType.BEACH) ? 1 : 0;

        if(ch <= 0.08f) { // Iron
            variant = VARIANT_IRON + add;
            health = 105.0f;
        } else if(ch <= 0.22) { // Coal
            variant = VARIANT_COAL + add;
            health = 90.0f;
        } else { // Regular
            variant = VARIANT_REG + add;
            health = 75.0f;
        }
    }

    @Override
    public void onDie() {
        SpawnItem coal = null;
        int rocksMin = 3;
        int rocksMax = 4;

        if(variant == VARIANT_COAL || variant == (VARIANT_COAL + 1)) {
            // Coal.
            rocksMin = 1;
            rocksMax = 2;
            coal = new SpawnItem("item_coal", 2, 3);
        } else if(variant == VARIANT_IRON || variant == (VARIANT_IRON + 1)) {
            // Iron.
            rocksMin = 1;
            rocksMax = 2;
            coal = new SpawnItem("item_iron_raw", 2, 3);
        }

        SpawnItem flint = null;

        if(MathUtils.random() <= 0.5f) {
            flint = new SpawnItem("item_flint", 1, 1);
        }

        spawnItemsAround(0, 4.875f, 11, 13,
                new SpawnItem("item_rock", rocksMin, rocksMax),
                coal,
                flint);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.BOULDER;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("variant", variant);
    }

    @Override
    public void onLoad(JSONObject saved) {
        variant = saved.getInt("variant");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {variant};
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return physicsBody;
    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.HEAVY;
    }

}
