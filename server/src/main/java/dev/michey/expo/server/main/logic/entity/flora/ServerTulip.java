package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import org.json.JSONObject;

public class ServerTulip extends ServerEntity {

    public int variant;

    public ServerTulip() {
        variant = 1;
        health = 20.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.FIST);
    }

    @Override
    public void onDie() {
        if(variant == 3 || variant == 4) {
            spawnItemsAround(1, 1, 0, 0, "item_tulip_red", 8);
        } else if(variant == 5 || variant == 6) {
            spawnItemsAround(1, 1, 0, 0, "item_tulip_orange", 8);
        }
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        variant = rnd.random(3, 6);
    }

    @Override
    public void onPlace(ServerInventoryItem item) {
        ItemMapping mapping = item.toMapping();

        if(mapping.identifier.equals("item_tulip_red")) {
            variant = MathUtils.random(3, 4);
        } else if(mapping.identifier.equals("item_tulip_orange")) {
            variant = MathUtils.random(5, 6);
        }
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.TULIP;
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
