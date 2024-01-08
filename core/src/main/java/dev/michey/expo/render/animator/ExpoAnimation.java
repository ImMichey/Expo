package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.util.AnimationSound;

import java.util.HashMap;
import java.util.Map;

public class ExpoAnimation {

    private ExpoAnimationHandler handler;
    private final Array<TextureRegion> textureArray;
    private final float frameDuration;
    private final float totalAnimationDuration;
    private float animationDelta;
    private float animationDeltaSinceStart;
    private boolean animationFinished;
    private HashMap<AnimationSound, Boolean> playSoundMap;
    private final float weight;

    public ExpoAnimation(String textureName, int frames, float frameDuration) {
        this(textureName, frames, frameDuration, 1.0f);
    }

    public ExpoAnimation(String textureName, int frames, float frameDuration, float weight) {
        textureArray = ExpoAssets.get().textureArray(textureName, frames);
        this.frameDuration = frameDuration;
        this.totalAnimationDuration = this.frameDuration * frames;
        this.weight = weight;
    }

    public void addAnimationSound(String soundGroupName, int frame, float volumeMultiplier) {
        if(playSoundMap == null) {
            playSoundMap = new HashMap<>();
        }

        playSoundMap.put(new AnimationSound(soundGroupName, frame, volumeMultiplier), false);
    }

    public void setHandler(ExpoAnimationHandler handler) {
        this.handler = handler;
    }

    public void randomOffset() {
        animationDelta = MathUtils.random(totalAnimationDuration);
    }

    public void tick(float delta) {
        animationFinished = false;
        animationDelta += delta;
        animationDeltaSinceStart += delta;

        if(playSoundMap != null) {
            int currentFrameIndex = getFrameIndex();

            for(Map.Entry<AnimationSound, Boolean> entrySet : playSoundMap.entrySet()) {
                AnimationSound as = entrySet.getKey();

                if(!entrySet.getValue()) {
                    if(currentFrameIndex >= as.animationIndex) {
                        handler.getEntity().playEntitySound(as.groupName, as.volumeMultiplier);
                        playSoundMap.put(as, true);
                    }
                }
            }
        }

        if(animationDeltaSinceStart >= totalAnimationDuration) {
            animationFinished = true;
            animationDeltaSinceStart -= totalAnimationDuration;
        }
    }

    public float getProgress() {
        return animationDelta / totalAnimationDuration;
    }

    public void reset() {
        animationDelta = 0;
        animationDeltaSinceStart = 0;
        if(playSoundMap != null) playSoundMap.replaceAll((p, v) -> false);
    }

    public void offset(float delta) {
        animationDelta = delta;
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
        /*
        if(pingPong) {
            int frameNumber = (int) (animationDelta / frameDuration);
            frameNumber %= this.textureArray.size * 2 - 2;

            if(frameNumber >= this.textureArray.size) {
                frameNumber = this.textureArray.size - 2 - (frameNumber - this.textureArray.size);
            }

            return frameNumber;
        } else {

        }
        */
    }

    public float getWeight() {
        return weight;
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
