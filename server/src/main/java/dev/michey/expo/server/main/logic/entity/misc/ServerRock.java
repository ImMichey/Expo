package dev.michey.expo.server.main.logic.entity.misc;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;
import org.json.JSONObject;

public class ServerRock extends ServerEntity {

    /** Physics body */
    private BoundingBox physicsBody;

    public int variant;

    public static final float[][] ROCK_BODIES = new float[][] {
            new float[] {1.5f, 3.5f, 21.0f, 10.0f},
            new float[] {1.5f, 3.0f, 11.5f, 4.5f},
            new float[] {1.0f, 2.0f, 8.5f, 3.0f},
    };

    public ServerRock() {
        variant = MathUtils.random(2, 4);
    }

    @Override
    public void onCreation() {
        // add physics body of player to world
        float[] b = ROCK_BODIES[variant - 2];
        physicsBody = new BoundingBox(this, b[0], b[1], b[2], b[3]);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        if(spread) {
            variant = MathUtils.random(3, 4);
        } else {
            variant = MathUtils.random(2, 4);
        }

        if(variant == 2) {
            health = 50.0f;
            damageableWith = ToolType.PICKAXE;
        } else if(variant == 3) {
            health = 20.0f;
        } else if(variant == 4) {
            health = 10.0f;
        }
    }

    private int variantToAmount() {
        return switch (variant) {
            case 2 -> MathUtils.random(2, 5);
            case 3 -> MathUtils.random(1, 2);
            default -> 1;
        };
    }

    @Override
    public void onDie() {
        int rocksSpawned = variantToAmount();
        Vector2[] positions = GenerationUtils.positions(rocksSpawned, 8.0f);
        float dspx = variant == 2 ? (11.5f) : (variant == 3 ? 5.5f : 5.0f);
        float dspy = variant == 2 ? (9.5f) : (variant == 3 ? 5.0f : 3.5f);

        for(int i = 0; i < rocksSpawned; i++) {
            ServerItem item = new ServerItem();

            ItemMapping r = ItemMapper.get().getMapping("item_rock");
            item.itemContainer = new ServerInventoryItem(r.id, 1);

            item.posX = posX + dspx;
            item.posY = posY + dspy;
            item.dstX = positions[i].x;
            item.dstY = positions[i].y;
            ServerWorld.get().registerServerEntity(entityDimension, item);
        }
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
