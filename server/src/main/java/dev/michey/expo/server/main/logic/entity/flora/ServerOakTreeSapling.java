package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import org.json.JSONObject;

public class ServerOakTreeSapling extends ServerEntity {

    private float growthTimeRemaining;

    public ServerOakTreeSapling() {
        health = 10.0f;
        growthTimeRemaining = MathUtils.random(8, 12);
    }

    @Override
    public void tick(float delta) {
        if(growthTimeRemaining > 0) {
            growthTimeRemaining -= delta;

            if(growthTimeRemaining <= 0) {
                // Convert to tree.
                killEntityWithPacket();

                ServerOakTree tree = new ServerOakTree();
                tree.posX = posX;
                tree.posY = posY;
                tree.setStaticEntity();
                tree.generateAge(null, null);
                tree.generateVariant(null);
                if(tileEntity) {
                    tree.attachToTile(getChunkGrid().getChunkSafe(chunkX, chunkY), tileX, tileY);
                }
                ServerWorld.get().registerServerEntity(entityDimension, tree);
            }
        }
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.OAK_TREE_SAPLING;
    }

    @Override
    public void onLoad(JSONObject saved) {
        growthTimeRemaining = saved.getFloat("gtr");
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("gtr", growthTimeRemaining);
    }

}