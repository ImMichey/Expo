package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.chunk.GenerationRandom;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.server.util.SpawnItem;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

public class ServerBlueberryBush extends ServerEntity {

    public boolean hasBerries;
    public float berryRegrowthDelta;

    public ServerBlueberryBush() {
        health = 40.0f;
        setDamageableWith(ToolType.SCYTHE, ToolType.AXE, ToolType.FIST);
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome, GenerationRandom rnd) {
        hasBerries = rnd.randomBoolean();
        berryRegrowthDelta = rnd.random(180f, 360f); // 3-6 min
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        if(hasBerries) {
            hasBerries = false;

            spawnItemsAround(0, 4.25f, 14, 18,
                    new SpawnItem("item_blueberry", 2, 3))
            ;

            ServerPackets.p30EntityDataUpdate(entityId, new Object[] {false}, PacketReceiver.whoCanSee(this));
            ServerPackets.p24PositionalSound("crab_snip", posX, posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));

            return false;
        }

        return true;
    }

    @Override
    public void tick(float delta) {
        if(!hasBerries && berryRegrowthDelta >= 0) {
            berryRegrowthDelta -= delta;

            if(berryRegrowthDelta <= 0) {
                berryRegrowthDelta = MathUtils.random(180f, 300f);
                hasBerries = true;
                ServerPackets.p30EntityDataUpdate(entityId, new Object[] {true}, PacketReceiver.whoCanSee(this));
            }
        }
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.BLUEBERRY_BUSH;
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("berries", hasBerries).add("regrowth", berryRegrowthDelta);
    }

    @Override
    public void onLoad(JSONObject saved) {
        hasBerries = saved.getBoolean("berries");
        berryRegrowthDelta = saved.getFloat("regrowth");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {hasBerries};
    }

}