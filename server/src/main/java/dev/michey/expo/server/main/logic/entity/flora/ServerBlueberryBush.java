package dev.michey.expo.server.main.logic.entity.flora;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.BoundingBox;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

public class ServerBlueberryBush extends ServerEntity {

    private BoundingBox physicsBody;
    public boolean hasBerries;
    public float berryRegrowthDelta;

    public ServerBlueberryBush() {
        health = 40.0f;
    }

    @Override
    public void onGeneration(boolean spread, BiomeType biome) {
        hasBerries = MathUtils.randomBoolean();
        berryRegrowthDelta = MathUtils.random(180f, 360f); // 3-6 min
    }

    @Override
    public void onCreation() {
        physicsBody = new BoundingBox(this, 0, 3, 15, 3.5f);
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        if(hasBerries) {
            hasBerries = false;

            int dropBerries = MathUtils.random(2, 3);
            Vector2[] positions = GenerationUtils.positions(dropBerries, 18.0f);

            for(int i = 0; i < dropBerries; i++) {
                ServerItem item = new ServerItem();

                ItemMapping r = ItemMapper.get().getMapping("item_blueberry");
                item.itemContainer = new ServerInventoryItem(r.id, 1);

                item.posX = posX + 7.5f;
                item.posY = posY + 3f;
                item.dstX = positions[i].x;
                item.dstY = positions[i].y;
                ServerWorld.get().registerServerEntity(entityDimension, item);
            }

            ServerPackets.p30EntityDataUpdate(entityId, new Object[] {false}, PacketReceiver.whoCanSee(this));
            ServerPackets.p24PositionalSound("pop", posX, posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(this));

            return false;
        }

        return true;
    }

    @Override
    public void tick(float delta) {
        if(!hasBerries && berryRegrowthDelta >= 0) {
            berryRegrowthDelta -= delta;

            if(berryRegrowthDelta <= 0) {
                berryRegrowthDelta = MathUtils.random(180f, 360f);
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
