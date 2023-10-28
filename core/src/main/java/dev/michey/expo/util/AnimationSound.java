package dev.michey.expo.util;

public class AnimationSound {

    public final String groupName;
    public final int animationIndex;
    public final float volumeMultiplier;

    public AnimationSound(String groupName, int animationIndex, float volumeMultiplier) {
        this.groupName = groupName;
        this.animationIndex = animationIndex;
        this.volumeMultiplier = volumeMultiplier;
    }

}