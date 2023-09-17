package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import org.json.JSONObject;

public class ServerCattail extends ServerEntity {

    public int variant;

    public ServerCattail() {
        variant = 1;
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        variant = rnd.random(1, 3);
    }

    @Override
    public void onDie() {
        spawnItemsAround(2, 3, 0, 6, "item_grassfiber", 8);
    }

    @Override
    public boolean postGeneration() {
        ServerTile tile = getCurrentTile();
        if(tile == null) return false;

        ServerTile[] neighbours = tile.getNeighbouringTiles();
        int waterTilesNearby = 1;
        boolean landNearby = false;

        for(ServerTile n : neighbours) {
            if(n == null) continue;

            if(!BiomeType.isWater(n.biome) && (n.biome == BiomeType.DENSE_FOREST || n.biome == BiomeType.FOREST || n.biome == BiomeType.PLAINS)) {
                landNearby = true;
            } else {
                waterTilesNearby++;
            }
        }

        return waterTilesNearby >= 2 && waterTilesNearby <= 6 && landNearby;
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.CATTAIL;
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
