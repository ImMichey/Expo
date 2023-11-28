package dev.michey.expo.server.main.logic.world.bbox;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;

public class KnockbackCalculation {

    /** Passed values */
    private final float knockbackStrength;
    private final float knockbackDuration;
    private final Vector2 knockbackDirection;

    /** Runtime values */
    private float elapsed;
    private float oldKnockbackX, oldKnockbackY;
    public float applyKnockbackX, applyKnockbackY;

    public KnockbackCalculation(float knockbackStrength, float knockbackDuration, Vector2 knockbackDirection) {
        this.knockbackStrength = knockbackStrength;
        this.knockbackDuration = knockbackDuration;
        this.knockbackDirection = knockbackDirection;
    }

    public boolean tick(float delta) {
        elapsed += delta;

        if(elapsed >= knockbackDuration) {
            elapsed = knockbackDuration;
        }

        float interpolated = Interpolation.pow3Out.apply(elapsed / knockbackDuration);

        float kx = interpolated * knockbackStrength * knockbackDirection.x;
        float ky = interpolated * knockbackStrength * knockbackDirection.y;

        applyKnockbackX = kx - oldKnockbackX;
        applyKnockbackY = ky - oldKnockbackY;

        oldKnockbackX = kx;
        oldKnockbackY = ky;

        return elapsed >= knockbackDuration;
    }

}
