package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import org.json.JSONObject;

public class ServerSign extends ServerEntity {

    public String text = "A sign";

    public ServerSign() {
        setDamageableWith(ToolType.FIST, ToolType.AXE);
    }

    @Override
    public void onDie() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.SIGN;
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void onLoad(JSONObject saved) {
        text = saved.getString("text");
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("text", text);
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {text};
    }

}