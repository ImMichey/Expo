package dev.michey.expo.logic.entity.particle;

import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.render.RenderContext;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientParticleHit extends ClientParticle {

    @Override
    public void onCreation() {
        particleTexture = ExpoAssets.get().getParticleSheet().randomHitParticle();
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        rc.useRegularBatch();
        int it = 10;

        rc.batch.setColor(r, g, b, useAlpha);
        rc.batch.draw(particleTexture, clientPosX, clientPosY);

        for(int i = 1; i <= it; i++) {
            float _a = i / (float) it * useAlpha;
            rc.batch.setColor(1f, 0f, 0f, _a);
            rc.batch.draw(particleTexture, clientPosX + 32 + i * 4, clientPosY);
        }

        rc.batch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.PARTICLE_HIT;
    }

}