package dev.michey.expo.render.animator;

import com.badlogic.gdx.math.MathUtils;

public class SquishAnimator2D {

    private final float duration;
    private final float x;
    private final float y;

    private boolean started;
    private boolean finished;
    private float delta;

    public float squishX;
    public float squishY;

    public SquishAnimator2D() {
        this.duration = 0.2f;
        this.x = 2.0f;
        this.y = 2.0f;
    }

    public SquishAnimator2D(float duration, float x, float y) {
        this.duration = duration;
        this.x = x;
        this.y = y;
    }

    public void reset() {
        started = true;
        finished = false;
        delta = 0;
        squishX = 0;
        squishY = 0;
    }

    public void calculate(float delta) {
        if(finished) return;
        if(!started) return;

        this.delta += delta;

        if(this.delta >= duration) {
            this.delta = duration;
            finished = true;
            started = false;
        }

        float v = MathUtils.cos(this.delta / duration * MathUtils.PI * 1.5f);
        squishX = v * x;
        squishY = -v * y;
    }

    public boolean isActive() {
        return started;
    }

}