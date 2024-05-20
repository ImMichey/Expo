package dev.michey.expo.render.animator;

import com.badlogic.gdx.math.MathUtils;

public class SimpleShakeAnimator {

    private float contactDelta = 0f;
    public float contactDir = 1f;

    public float SPEED = 3.5f;
    public float STRENGTH = 3.5f;
    public float STRENGTH_DECREASE = 0.7f;
    public int STEPS = 5; // step = 0.5f (half radiant)

    public float value;

    public void calculate(float delta) {
        if(contactDelta != 0) {
            contactDelta -= delta * SPEED;
            if(contactDelta < 0f) contactDelta = 0f;

            float full = STEPS * 0.5f;
            float diff = full - contactDelta;
            int decreases = (int) (diff / 0.5f);
            float useStrength = STRENGTH - STRENGTH_DECREASE * decreases;

            value = useStrength * contactDir * MathUtils.sin(((STEPS * 0.5f) - contactDelta) * MathUtils.PI2);
        } else {
            value = 0;
        }
    }

    public void reset(float delta) {
        this.contactDelta = delta;
    }

}
