package dev.michey.expo.logic.entity.particle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.render.RenderContext;

public class ClientParticleFood extends ClientParticle {

    private TextureRegion subTexture;

    @Override
    public void onCreation() {
        float w = particleTexture.getRegionWidth();
        float h = particleTexture.getRegionHeight();

        int pw = 2;
        int ph = 2;

        int remainderW = (int) w - pw;
        int remainderH = (int) h - ph;

        subTexture = new TextureRegion(particleTexture, MathUtils.random(remainderW), MathUtils.random(remainderH), pw, ph);
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

        float w = subTexture.getRegionWidth() * scaleX;
        float h = subTexture.getRegionHeight() * scaleY;

        rc.arraySpriteBatch.draw(subTexture, clientPosX, clientPosY, w * 0.5f, h * 0.5f, w, h, 1.0f, 1.0f, rotation);
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.PARTICLE_FOOD;
    }

}
