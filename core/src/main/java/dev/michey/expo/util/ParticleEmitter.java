package dev.michey.expo.util;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.render.RenderContext;

public class ParticleEmitter {

    private ClientEntity linkedEntity = null;
    private final ParticleBuilder builder;
    private final float cooldownMin, cooldownMax;

    private float spawnNextDelta;

    public ParticleEmitter(ParticleBuilder builder, float delay, float cooldownMin, float cooldownMax) {
        this.builder = builder;
        this.cooldownMin = cooldownMin;
        this.cooldownMax = cooldownMax;
        this.spawnNextDelta = delay;
    }

    public void setLinkedEntity(ClientEntity entity) {
        this.linkedEntity = entity;
    }

    public void tick(float delta) {
        if(!GameSettings.get().enableParticles) return;
        if(RenderContext.get().expoCamera.camera.zoom >= 1f) return;
        if(linkedEntity != null && !linkedEntity.visibleToRenderEngine) return;
        spawnNextDelta -= delta;

        if(spawnNextDelta <= 0) {
            spawnNextDelta = MathUtils.random(cooldownMin, cooldownMax);
            builder.spawn();
        }
    }

}
