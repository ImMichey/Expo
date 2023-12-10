package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ExpoAnimationHandler {

    private final ClientEntity parent;
    private final HashMap<String, List<ExpoAnimation>> animationMap;
    private ExpoAnimation activeAnimation;
    private String activeName;
    private int lastFootstepIndex;
    private String[] footstepAnimations;
    private int[] footstepIndexes;
    private boolean cachedMoving;

    public ExpoAnimationHandler(ClientEntity parent) {
        this.parent = parent;
        animationMap = new HashMap<>();
    }

    public void addAnimation(String name, ExpoAnimation animation) {
        if(!animationMap.containsKey(name)) animationMap.put(name, new LinkedList<>());
        animationMap.get(name).add(animation);
        animation.setHandler(this);

        if(animationMap.size() == 1) {
            activeName = name;
            activeAnimation = animation;
        }
    }

    public void addAnimationSound(String name, String soundGroupName, int frame, float volumeMultiplier) {
        for(ExpoAnimation an : animationMap.get(name)) {
            an.addAnimationSound(soundGroupName, frame, volumeMultiplier);
        }
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
            switchToAnimation(cachedMoving ? "walk" : "idle");
        } else {
            switchToAnimation(getActiveAnimationName());
        }
    }

    private void checkForFlip(float xDir) {
        boolean flip = (!parent.flipped && xDir <= 0) || (parent.flipped && xDir == 1);

        if(flip) {
            flipAllAnimations(true, false);
            parent.flipped = !parent.flipped;
        }
    }

    public void flipAllAnimations(boolean x, boolean y) {
        for(List<ExpoAnimation> animation : animationMap.values()) {
            for(ExpoAnimation a : animation) a.flip(x, y);
        }
    }

    public void switchToAnimation(String name) {
        var list = animationMap.get(name);

        if(list.size() == 1) {
            activeAnimation = list.get(0);
        } else {
            ExpoAnimation use = null;
            float totalWeight = 0;

            for(ExpoAnimation an : list) {
                totalWeight += an.getWeight();
            }

            float calculatedWeight = MathUtils.random(0f, totalWeight);
            float countWeight = 0f;

            for(ExpoAnimation an : list) {
                countWeight += an.getWeight();

                if(countWeight >= calculatedWeight) {
                    use = an;
                    break;
                }
            }

            activeAnimation = use;
        }

        activeName = name;
    }

    public void reset() {
        for(List<ExpoAnimation> animation : animationMap.values()) {
            for(ExpoAnimation a : animation) a.reset();
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