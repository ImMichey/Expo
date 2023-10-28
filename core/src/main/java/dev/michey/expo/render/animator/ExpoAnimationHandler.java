package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.util.ExpoShared;

import java.util.HashMap;

public class ExpoAnimationHandler {

    private final ClientEntity parent;
    private final HashMap<String, ExpoAnimation> animationMap;
    private ExpoAnimation activeAnimation;
    private String activeName;
    private boolean flipped;
    private int lastFootstepIndex;
    private String[] footstepAnimations;
    private int[] footstepIndexes;
    private boolean cachedMoving;

    public ExpoAnimationHandler(ClientEntity parent) {
        this.parent = parent;
        animationMap = new HashMap<>();
    }

    public void addAnimation(String name, ExpoAnimation animation) {
        animationMap.put(name, animation);
        animation.setHandler(this);

        if(animationMap.size() == 1) {
            activeName = name;
            activeAnimation = animation;
        }
    }

    public void addAnimationSound(String name, String soundGroupName, int frame, float volumeMultiplier) {
        animationMap.get(name).addAnimationSound(soundGroupName, frame, volumeMultiplier);
    }

    public void addAnimationSound(String name, String soundGroupName, int frame) {
        addAnimationSound(name, soundGroupName, frame, 1.0f);
    }

    public void tick(float delta) {
        activeAnimation.tick(delta);

        if(!activeName.equals("attack") && cachedMoving != parent.isMoving()) {
            cachedMoving = !cachedMoving;
            reset();
            switchToAnimation(cachedMoving ? "walk" : "idle");
        }

        checkForFlip(parent.serverDirX);

        if(footstepIndexes != null) {
            for(String fan : footstepAnimations) {
                if(fan.equals(getActiveAnimationName())) {
                    int currentIndex = getActiveAnimation().getFrameIndex();

                    for(int i : footstepIndexes) {
                        if(i == currentIndex && lastFootstepIndex != i) {
                            lastFootstepIndex = i;
                            parent.playFootstepSound();
                        }
                    }
                    break;
                }
            }
        }

        if(activeAnimation.isAnimationFinished()) {
            onAnimationFinish();
        }
    }

    public void addFootstepOn(String[] footstepAnimations, int... footstepIndexes) {
        this.footstepAnimations = footstepAnimations;
        this.footstepIndexes = footstepIndexes;
    }

    public void onAnimationFinish() {
        lastFootstepIndex = 0;

        if(getActiveAnimationName().equals("attack")) {
            switchToAnimation("idle");
        }
    }

    private void checkForFlip(float xDir) {
        boolean flip = (!flipped && xDir <= 0) || (flipped && xDir == 1);

        if(flip) {
            flipAllAnimations(true, false);
            flipped = !flipped;
        }
    }

    public void flipAllAnimations(boolean x, boolean y) {
        for(ExpoAnimation animation : animationMap.values()) animation.flip(x, y);
    }

    public void switchToAnimation(String name) {
        activeAnimation = animationMap.get(name);
        activeName = name;
    }

    public void reset() {
        for(ExpoAnimation animation : animationMap.values()) {
            animation.reset();
        }
    }

    public ExpoAnimation getActiveAnimation() {
        return activeAnimation;
    }

    public TextureRegion getActiveFrame() {
        return getActiveAnimation().getFrame();
    }

    public String getActiveAnimationName() {
        return activeName;
    }

    public ClientEntity getEntity() {
        return parent;
    }

}