package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import org.json.JSONObject;

public class ServerGravestone extends ServerEntity {

    public String owner;

    public ServerGravestone() {
        setDamageableWith(ToolType.FIST, ToolType.PICKAXE);
    }

    @Override
    public void onDie() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.GRAVESTONE;
    }

    @Override
    public void tick(float delta) {
        if(invincibility > 0) invincibility -= delta;
    }

    @Override
    public void onLoad(JSONObject saved) {
        owner = saved.getString("owner");
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("owner", owner);
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {owner};
    }

    @Override
    public boolean onDamage(ServerEntity damageSource, float damage) {
        if(invincibility > 0) return false;
        return super.onDamage(damageSource, damage);
    }
}
