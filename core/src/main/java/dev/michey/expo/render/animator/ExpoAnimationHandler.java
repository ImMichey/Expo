package dev.michey.expo.render.animator;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

public class ExpoAnimationHandler {

    private final HashMap<String, ExpoAnimation> animationMap;
    private ExpoAnimation activeAnimation;
    private String activeName;

    public ExpoAnimationHandler() {
        animationMap = new HashMap<>();
    }

    public void addAnimation(String name, ExpoAnimation animation) {
        animationMap.put(name, animation);

        if(animationMap.size() == 1) {
            activeName = name;
            activeAnimation = animation;
        }
    }

    public void tick(float delta) {
        activeAnimation.tick(delta);
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

}
