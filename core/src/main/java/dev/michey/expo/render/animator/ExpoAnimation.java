package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;

public class ExpoAnimation {

    private final Array<TextureRegion> textureArray;
    private final float frameDuration;
    private float animationDelta;

    public ExpoAnimation(String textureName, int frames, float frameDuration) {
        textureArray = ExpoAssets.get().textureArray(textureName, frames);
        this.frameDuration = frameDuration;
    }

    public void tick(float delta) {
        animationDelta += delta;
    }

    public void reset() {
        animationDelta = 0;
    }

    public void flip(boolean x, boolean y) {
        for(TextureRegion t : textureArray) t.flip(x, y);
    }

    public TextureRegion getFrame() {
        int index = (int) (animationDelta / frameDuration) % textureArray.size;
        return textureArray.get(index);
    }

}
