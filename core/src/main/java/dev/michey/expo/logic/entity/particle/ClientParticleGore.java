package dev.michey.expo.logic.entity.particle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.render.RenderContext;

public class ClientParticleGore extends ClientParticle {

    public TextureRegion subTexture;

    @Override
    public void onCreation() {
        float w = particleTexture.getRegionWidth();
        float h = particleTexture.getRegionHeight();

        float u = particleTexture.getU();       // x1
        float u2 = particleTexture.getU2();     // x2 -> can be flipped

        float v = particleTexture.getV();       // y1
        float v2 = particleTexture.getV2();     // y2

        float uPerPx = (u2 - u) / w;
        float vPerPx = (v2 - v) / h;

        float ww = MathUtils.random(2f, 5f);
        float hh = MathUtils.random(2f, 5f);

        float off_x = MathUtils.random(w - ww) * uPerPx;
        float off_y = MathUtils.random(h - hh) * vPerPx;

        ww *= uPerPx;
        hh *= vPerPx;

        subTexture = new TextureRegion(particleTexture.getTexture(), u + off_x, v + off_y, u + off_x + ww, v + off_y + hh);
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
        rc.useRegularArrayShader();
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
        return ClientEntityType.PARTICLE_GORE;
    }

}
