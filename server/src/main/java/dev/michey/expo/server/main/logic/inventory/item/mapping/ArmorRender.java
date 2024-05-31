package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.json.JSONObject;

public class ArmorRender {

    public String textureIdle;
    public String textureWalk;
    public TextureRegion[] idleFrames;
    public TextureRegion[] walkFrames;
    public float offsetX;
    public float offsetY;
    public float scaleX;
    public float scaleY;
    public boolean hideLayerBelow;

    public ArmorRender(JSONObject object) {
        textureIdle = object.getString("texture_idle");
        textureWalk = object.getString("texture_walk");

        if(object.has("scaleX")) {
            scaleX = object.getFloat("scaleX");
        } else {
            scaleX = 1.0f;
        }

        if(object.has("scaleY")) {
            scaleY = object.getFloat("scaleY");
        } else {
            scaleY = 1.0f;
        }

        if(object.has("offsetX")) {
            offsetX = object.getFloat("offsetX");
        }

        if(object.has("offsetY")) {
            offsetY = object.getFloat("offsetY");
        }

        if(object.has("hideLayerBelow")) {
            hideLayerBelow = object.getBoolean("hideLayerBelow");
        }
    }

    public ArmorRender(ArmorRender existing) {
        this.textureIdle = existing.textureIdle;
        this.textureWalk = existing.textureWalk;
        this.offsetX = existing.offsetX;
        this.offsetY = existing.offsetY;
        this.scaleX = existing.scaleX;
        this.scaleY = existing.scaleY;
        this.hideLayerBelow = existing.hideLayerBelow;
    }

    public void flip() {
        for(TextureRegion t : idleFrames) t.flip(true, false);
        for(TextureRegion t : walkFrames) t.flip(true, false);
    }

    public ArmorRender copy() {
        return new ArmorRender(this);
    }

}
