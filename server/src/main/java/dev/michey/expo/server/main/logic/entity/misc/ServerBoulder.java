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

    public static final float[][] ROCK_BODIES = new float[][] {
            new float[] {-9.0f, 2.0f, 18.0f, 6.0f},
            new float[] {-9.0f, 2.0f, 18.0f, 6.0f},
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
        if(rnd.random() <= 0.25f) {
            variant = 2;
            health = 80.0f;
        } else {
            variant = 1;
            health = 50.0f;
        }
    }

    @Override
    public void onDie() {
        SpawnItem coal = null;
        int rocksMin = 3;
        int rocksMax = 5;

        if(variant == 2) {
            // Coal.
            rocksMin = 2;
            rocksMax = 3;
            coal = new SpawnItem("item_coal", 2, 3);
        }

        spawnItemsAround(0, 4.875f, 8, 12,
                new SpawnItem("item_rock", rocksMin, rocksMax),
                coal);
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
    public void onMoved() {

    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.HEAVY;
    }

}
