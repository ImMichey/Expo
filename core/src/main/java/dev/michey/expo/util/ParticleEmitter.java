package dev.michey.expo.util;

import com.badlogic.gdx.math.MathUtils;

public class ParticleEmitter {

    private final ParticleBuilder builder;
    private final float cooldownMin, cooldownMax;

    private float spawnNextDelta;

    public ParticleEmitter(ParticleBuilder builder, float delay, float cooldownMin, float cooldownMax) {
        this.builder = builder;
        this.cooldownMin = cooldownMin;
        this.cooldownMax = cooldownMax;
        this.spawnNextDelta = delay;
    }

    public void tick(float delta) {
        spawnNextDelta -= delta;

        if(spawnNextDelta <= 0) {
            spawnNextDelta = MathUtils.random(cooldownMin, cooldownMax);
            builder.spawn();
        }
    }

}
