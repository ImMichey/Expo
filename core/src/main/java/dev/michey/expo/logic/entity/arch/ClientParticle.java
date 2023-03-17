package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public abstract class ClientParticle extends ClientEntity {

    public TextureRegion particleTexture;

    public float r, g, b, a;
    public float lifetime;
    public float rotation;
    public float scaleX = 1.0f, scaleY = 1.0f;
    public boolean fadeOut;
    public float fadeOutDuration;
    public float useAlpha;

    //private float pox, poy; // Particle origin values
    private float pvx, pvy; // Particle velocity values

    @Override
    public void tick(float delta) {
        lifetime -= delta;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
        }

        if(fadeOut) {
            if(lifetime <= fadeOutDuration) {
                useAlpha = (lifetime / fadeOutDuration);
                if(useAlpha < 0) useAlpha = 0;
            }
        }

        if(pvx != 0) clientPosX += pvx * delta;
        if(pvy != 0) clientPosY += pvy * delta;

        updateDepth();
    }

    public void setParticleLifetime(float lifetime) {
        this.lifetime = lifetime;
    }

    public void setParticleOriginAndVelocity(float pox, float poy, float pvx, float pvy) {
        this.pvx = pvx;
        this.pvy = pvy;
        clientPosX = pox;
        clientPosY = poy;
    }

    public void setParticleRotation(float rotation) {
        this.rotation = rotation;
    }

    public void setParticleColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        useAlpha = a;
    }

    public void setParticleColor(float r, float g, float b) {
        setParticleColor(r, g, b, 1.0f);
    }

    public void setParticleScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void setParticleColor(Color c) {
        setParticleColor(c.r, c.g, c.b, c.a);
    }

    public void setParticleFadeout(float fadeDuration) {
        fadeOut = true;
        this.fadeOutDuration = fadeDuration;
    }

}
