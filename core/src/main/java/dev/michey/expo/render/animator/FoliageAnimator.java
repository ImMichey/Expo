package dev.michey.expo.render.animator;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;

public class FoliageAnimator {

    private final float u_speed;
    private final float u_offset;
    private final float u_minStrength;
    private final float u_maxStrength;
    private final float u_interval;
    private final float u_detail;

    public float value;
    private boolean calculatedWindThisTick = false;

    public FoliageAnimator(float minSpeed, float maxSpeed, float minStrength1, float minStrength2, float maxStrength1, float maxStrength2, float minInterval, float maxInterval, float minDetail, float maxDetail) {
        u_speed = MathUtils.random(minSpeed, maxSpeed);
        u_offset = MathUtils.random(100f);
        u_minStrength = MathUtils.random(minStrength1, minStrength2);
        u_maxStrength = MathUtils.random(maxStrength1, maxStrength2);
        u_interval = MathUtils.random(minInterval, maxInterval);
        u_detail = MathUtils.random(minDetail, maxDetail);
    }

    public FoliageAnimator() {
        // Default values for grass
        this(0.5f, 1.2f, 0.02f, 0.04f, 0.04f, 0.06f, 2.0f, 5.0f, 0.5f, 1.5f);
    }

    public void resetWind() {
        calculatedWindThisTick = false;
    }

    public void calculateWindOnDemand() {
        if(!calculatedWindThisTick) {
            calculatedWindThisTick = true;
            value = ShadowUtils.getWind(u_maxStrength, u_minStrength, RenderContext.get().deltaTotal * u_speed + u_offset, u_interval, u_detail);
        }
    }

}
