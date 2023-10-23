package dev.michey.expo.logic.entity.particle;

import com.badlogic.gdx.graphics.Color;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.render.RenderContext;

public class ClientParticleHit extends ClientParticle {

    @Override
    public void onCreation() {
        particleTexture = ExpoAssets.get().getParticleSheet().getRandomParticle(particleRangeStart, particleRangeEnd);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        rc.useArrayBatch();
        rc.arraySpriteBatch.setColor(r, g, b, useAlpha);

        float w = particleTexture.getRegionWidth() * scaleX;
        float h = particleTexture.getRegionHeight() * scaleY;

        rc.arraySpriteBatch.draw(particleTexture, clientPosX - w * 0.5f, clientPosY - h * 0.5f, w * 0.5f, h * 0.5f, w, h, 1.0f, 1.0f, rotation);
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.PARTICLE_HIT;
    }

}
