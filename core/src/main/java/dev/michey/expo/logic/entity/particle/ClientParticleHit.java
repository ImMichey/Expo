package dev.michey.expo.logic.entity.particle;

import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.render.RenderContext;

public class ClientParticleHit extends ClientParticle {

    @Override
    public void onCreation() {
        particleTexture = ExpoAssets.get().getParticleSheet().getRandomParticle(3, 7);
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
        rc.batch.setColor(r, g, b, useAlpha);

        if(rotationSpeed > 0) {
            rotation += delta * rotationSpeed;
        }

        rc.batch.draw(particleTexture, clientPosX, clientPosY, 0, 0, particleTexture.getRegionWidth(), particleTexture.getRegionHeight(), scaleX, scaleY, rotation);
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
