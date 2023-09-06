package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;

public class ExpoAnimation {

    private final Array<TextureRegion> textureArray;
    private final float frameDuration;
    private final float totalAnimationDuration;
    private float animationDelta;
    private float animationDeltaSinceStart;
    private boolean animationFinished;

    public ExpoAnimation(String textureName, int frames, float frameDuration) {
        textureArray = ExpoAssets.get().textureArray(textureName, frames);
        this.frameDuration = frameDuration;
        this.totalAnimationDuration = this.frameDuration * frames;
    }

    public void tick(float delta) {
        animationFinished = false;
        animationDelta += delta;
        animationDeltaSinceStart += delta;

        if(animationDeltaSinceStart >= totalAnimationDuration) {
            animationFinished = true;
            animationDeltaSinceStart -= totalAnimationDuration;
        }
    }

    public void reset() {
        animationDelta = 0;
        animationDeltaSinceStart = 0;
    }

    public void flip(boolean x, boolean y) {
        for(TextureRegion t : textureArray) t.flip(x, y);
    }

    public TextureRegion getFrame() {
        return getFrame(getFrameIndex());
    }

    public TextureRegion getFrame(int index) {
        return textureArray.get(index);
    }

    public int getFrameIndex() {
        return (int) (animationDelta / frameDuration) % textureArray.size;
    }

    public boolean isAnimationFinished() {
        return animationFinished;
        //return animationDelta >= totalAnimationDuration;
    }

    public float getDelta() {
        return animationDelta;
    }

}
