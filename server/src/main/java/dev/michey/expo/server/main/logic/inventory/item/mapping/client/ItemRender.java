package dev.michey.expo.server.main.logic.inventory.item.mapping.client;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.json.JSONObject;

public class ItemRender {

    public String texture;
    public TextureRegion textureRegion = null; // will be grabbed by client instance
    public float scaleX;
    public float scaleY;
    public int offsetX;
    public int offsetY;

    public ItemRender(JSONObject object) {
        texture = object.getString("texture");
        scaleX = object.getFloat("scaleX");
        scaleY = object.getFloat("scaleY");
        if(object.has("offsetX") && object.has("offsetY")) {
            offsetX = object.getInt("offsetX");
            offsetY = object.getInt("offsetY");
        }
    }

    public void setTextureRegion(TextureRegion region) {
        this.textureRegion = region;
    }

}
