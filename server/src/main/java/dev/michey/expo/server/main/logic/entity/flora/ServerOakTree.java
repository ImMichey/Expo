package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
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
        new float[] {2.0f, 4.0f, 15.0f, 4.5f}
    };

    @Override
    public void onCreation() {
        // add physics body of player to world
        float[] b = TREE_BODIES[variant - 1];
        physicsBody = new BoundingBox(this, b[0], b[1], b[2], b[3]);
    }

    @Override
    public void onGeneration() {
        generateAge();
        generateVariant();
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

    public void generateAge() {
        age = MathUtils.random(0, 2);
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