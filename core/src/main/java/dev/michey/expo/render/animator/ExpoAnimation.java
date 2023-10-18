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
    private boolean pingPong;

    public ExpoAnimation(String textureName, int frames, float frameDuration) {
        this(textureName, frames, frameDuration, false);
    }

    public ExpoAnimation(String textureName, int frames, float frameDuration, boolean pingPong) {
        textureArray = ExpoAssets.get().textureArray(textureName, frames, pingPong);
        this.frameDuration = frameDuration;
        this.totalAnimationDuration = this.frameDuration * frames;
    }

    public void tick(float delta) {
        animationFinished = false;
        animationDelta += delta;
        animationDeltaSinceStart += delta;

        if(animationDeltaSinceStart >= (totalAnimationDuration * (pingPong ? 2 : 1))) {
            animationFinished = true;
            animationDeltaSinceStart -= totalAnimationDuration;
        }
    }

    public void reset() {
        animationDelta = 0;
        animationDeltaSinceStart = 0;
    }

    public void offset(float delta) {
        animationDelta = delta;
    }

    public void setPingPong() {
        pingPong = true;
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
        if(pingPong) {
            int frameNumber = (int) (animationDelta / frameDuration);
            frameNumber %= this.textureArray.size * 2 - 2;

            if(frameNumber >= this.textureArray.size) {
                frameNumber = this.textureArray.size - 2 - (frameNumber - this.textureArray.size);
            }

            return frameNumber;
        } else {
            return (int) (animationDelta / frameDuration) % textureArray.size;
        }
    }

    public boolean isAnimationFinished() {
        return animationFinished;
    }

    public Array<TextureRegion> getTextureArray() {
        return textureArray;
    }

    public float getDelta() {
        return animationDelta;
    }

}
