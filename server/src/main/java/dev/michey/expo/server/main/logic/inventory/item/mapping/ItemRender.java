package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.json.JSONArray;
import org.json.JSONObject;

public class ItemRender {

    public String texture;
    public TextureRegion[] textureRegions = null; // will be grabbed by client instance
    public TextureRegion useTextureRegion = null;
    public float useWidth;
    public float useHeight;

    // optional
    public float scaleX;
    public float scaleY;
    public float offsetX;
    public float offsetY;
    public float[] rotations;
    public boolean rotationLock;
    public boolean renderPriority;
    public boolean requiresFlip;
    public float animationSpeed;
    public int animationFrames;
    public float animationDelta;
    public boolean updatedAnimation;
    public ItemRenderLight renderLight;
    public ItemRenderParticleEmitter particleEmitter;
    public ItemRenderSoundEmitter soundEmitter;
    public boolean hideShadow;

    public ItemRender(JSONObject object) {
        texture = object.getString("texture");

        if(object.has("frames")) {
            animationFrames = object.getInt("frames");
        }

        if(object.has("animationSpeed")) {
            animationSpeed = object.getFloat("animationSpeed");
        }

        if(object.has("scaleX") && object.has("scaleY")) {
            scaleX = object.getFloat("scaleX");
            scaleY = object.getFloat("scaleY");
        } else {
            scaleX = 1.0f;
            scaleY = 1.0f;
        }

        if(object.has("rotationLock")) {
            rotationLock = object.getBoolean("rotationLock");
        } else {
            rotationLock = false;
        }

        if(object.has("offsetX") && object.has("offsetY")) {
            offsetX = object.getFloat("offsetX");
            offsetY = object.getFloat("offsetY");
        }

        if(object.has("useWidth")) {
            useWidth = object.getFloat("useWidth");
        }

        if(object.has("useHeight")) {
            useHeight = object.getFloat("useHeight");
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

        if(object.has("hideShadow")) {
            hideShadow = object.getBoolean("hideShadow");
        } else {
            hideShadow = false;
        }

        if(object.has("light")) {
            renderLight = new ItemRenderLight(object.getJSONObject("light"));
        } else {
            renderLight = null;
        }

        if(object.has("particleEmitter")) {
            particleEmitter = new ItemRenderParticleEmitter(object.getJSONObject("particleEmitter"));
        } else {
            particleEmitter = null;
        }

        if(object.has("soundEmitter")) {
            soundEmitter = new ItemRenderSoundEmitter(object.getJSONObject("soundEmitter"));
        } else {
            soundEmitter = null;
        }
    }

    public void flip() {
        for(TextureRegion t : textureRegions) t.flip(true, false);
    }

}