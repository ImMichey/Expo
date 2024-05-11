package dev.michey.expo.render.animator;

import com.badlogic.gdx.math.MathUtils;

public class SquishAnimatorAdvanced2D {

    private final float duration;
    private final float x;
    private final float y;

    private boolean started;
    private boolean finished;
    private float delta;

    public float squishX1;
    public float squishX2;

    public float squishY1;
    public float squishY2;

    public SquishAnimatorAdvanced2D() {
        this.duration = 0.2f;
        this.x = 2.0f;
        this.y = 2.0f;
    }

    public SquishAnimatorAdvanced2D(float duration, float x, float y) {
        this.duration = duration;
        this.x = x;
        this.y = y;
    }

    public void reset() {
        started = true;
        finished = false;
        delta = 0;
        squishX1 = 0;
        squishX2 = 0;
        squishY1 = 0;
        squishY2 = 0;
    }

    public void calculate(float delta) {
        if(finished) {
            finished = false;
            return;
        }
        if(!started) return;

        this.delta += delta;

        if(this.delta >= duration) {
            finished = true;
            started = false;
            squishX1 = 0;
            squishX2 = 0;
            squishY1 = 0;
            squishY2 = 0;
        } else {
            float v = MathUtils.cos(this.delta / duration * MathUtils.PI * 1.5f);
            squishX1 = v * x * 0.5f;
            squishX2 = v * x;
            squishY1 = v * y * 0.5f;
            squishY2 = -v * y;
        }
    }

    public boolean isActive() {
        return started;
    }

    public boolean isFinished() {
        return finished;

    }
}