package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.util.AnimationSound;
import dev.michey.expo.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ExpoAnimation {

    private ExpoAnimationHandler handler;
    private final Array<TextureRegion> textureArray;
    private final float frameDuration;
    private final float totalAnimationDuration;
    private float animationDelta;
    private float animationDeltaSinceStart;
    private boolean animationFinished;
    private boolean pingPong;
    private HashMap<AnimationSound, Boolean> playSoundMap;

    public ExpoAnimation(String textureName, int frames, float frameDuration) {
        this(textureName, frames, frameDuration, false);
    }

    public ExpoAnimation(String textureName, int frames, float frameDuration, boolean pingPong) {
        textureArray = ExpoAssets.get().textureArray(textureName, frames, pingPong);
        this.frameDuration = frameDuration;
        this.totalAnimationDuration = this.frameDuration * frames;
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

    public void tick(float delta) {
        animationFinished = false;
        animationDelta += delta;
        animationDeltaSinceStart += delta;

        if(playSoundMap != null) {
            int currentFrameIndex = getFrameIndex();

            for(AnimationSound as : playSoundMap.keySet()) {
                if(!playSoundMap.get(as)) {
                    if(currentFrameIndex >= as.animationIndex) {
                        handler.getEntity().playEntitySound(as.groupName, as.volumeMultiplier);
                        playSoundMap.put(as, true);
                    }
                }
            }
        }

        if(animationDeltaSinceStart >= (totalAnimationDuration * (pingPong ? 2 : 1))) {
            animationFinished = true;
            animationDeltaSinceStart -= totalAnimationDuration;
        }
    }

    public void reset() {
        animationDelta = 0;
        animationDeltaSinceStart = 0;
        if(playSoundMap != null) playSoundMap.replaceAll((p, v) -> false);
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
