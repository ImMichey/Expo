package dev.michey.expo.server.main.logic.inventory.item.mapping.client;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.json.JSONArray;
import org.json.JSONObject;

public class ItemRender {

    public String texture;
    public TextureRegion textureRegion = null; // will be grabbed by client instance

    // optional
    public float scaleX;
    public float scaleY;
    public float offsetX;
    public float offsetY;
    public float[] rotations;
    public boolean renderPriority;
    public boolean requiresFlip;

    public ItemRender(JSONObject object) {
        texture = object.getString("texture");
        if(object.has("scaleX") && object.has("scaleY")) {
            scaleX = object.getFloat("scaleX");
            scaleY = object.getFloat("scaleY");
        } else {
            scaleX = 1.0f;
            scaleY = 1.0f;
        }
        if(object.has("offsetX") && object.has("offsetY")) {
            offsetX = object.getFloat("offsetX");
            offsetY = object.getFloat("offsetY");
        }
        if(object.has("rotation")) {
            float r = object.getFloat("rotation");
            rotations = new float[] {r, r};
        } else if(object.has("rotations")) {
            JSONArray array = object.getJSONArray("rotations");
            rotations = new float[2];
            for(int i = 0; i < array.length(); i++) {
                rotations[i] = array.getFloat(i);
            }
        } else {
            rotations = new float[] {0, 0};
        }
        if(object.has("renderPriority")) {
            renderPriority = object.getBoolean("renderPriority");
        }
        if(object.has("requiresFlip")) {
            requiresFlip = object.getBoolean("requiresFlip");
        } else {
            requiresFlip = true;
        }
    }

    public void setTextureRegion(TextureRegion region) {
        this.textureRegion = region;
    }

}
