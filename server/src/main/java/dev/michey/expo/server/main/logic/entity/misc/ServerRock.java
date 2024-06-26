package dev.michey.expo.server.main.logic.entity.misc;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import org.json.JSONObject;

public class ServerRock extends ServerEntity {

    /** Physics body */
    private EntityPhysicsBox physicsBody;

    public int variant = 1;

    public static final float[][] ROCK_BODIES = new float[][] {
            new float[] {-3.5f, 1.0f, 7.0f, 3.0f},
            new float[] {-5.0f, 2.0f, 10.0f, 4.0f},
            new float[] {-4.5f, 2.0f, 9.0f, 4.0f},
            new float[] {-3.5f, 1.0f, 7.0f, 3.0f},
            new float[] {-5.0f, 2.0f, 10.0f, 4.0f},
            new float[] {-4.5f, 2.0f, 9.0f, 4.0f},
    };

    @Override
    public void onCreation() {
        // add physics body of player to world
        //float[] b = ROCK_BODIES[variant - 1];
        //physicsBody = new EntityPhysicsBox(this, b[0], b[1], b[2], b[3]);
    }

    @Override
    public void onDeletion() {
        //physicsBody.dispose();
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        int start = (biome == BiomeType.DESERT || biome == BiomeType.BEACH) ? 4 : 1;
        variant = rnd.random(start, start + 1);
        health = 10.0f;
    }

    @Override
    public void onDie() {
        float[] data = itemData();
        boolean flint = MathUtils.random() <= 0.5f;

        if(flint) {
            spawnItemsAround((int) data[0], (int) data[1], data[2], data[3], "item_flint", 8);
        } else {
            spawnItemsAround((int) data[0], (int) data[1], data[2], data[3], "item_rock", 8);
        }
    }

    private float[] itemData() {
        float v1y = 7.0f; float v2y = 9.0f; float v3y = 11.0f;
        float fh = 5.25f; // Flint Texture * 0.75 Scale
        float rh = 7.5f; // Rock Texture * 0.75 Scale
        if(variant == 3 || variant == 6) return new float[] {1, 2, 0, (v3y - fh) * 0.5f};
        if(variant == 1 || variant == 4) return new float[] {1, 2, 0, (v1y - rh) * 0.5f};
        return new float[] {1, 2, 0, (v2y - rh) * 0.5f};
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.ROCK;
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

}
