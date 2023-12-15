package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;

public abstract class ClientParticle extends ClientEntity {

    public TextureRegion particleTexture;
    public int particleRangeStart, particleRangeEnd;

    public float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;
    public float lifetime;
    public float lifetimeStatic;
    public float rotation;
    public float scaleX = 1.0f, scaleY = 1.0f;
    public boolean fadeIn;
    public float fadeInDuration;
    public boolean fadeOut;
    public float fadeOutDuration;
    public float useAlpha;
    public float rotationSpeed;
    public boolean dynamicDepth;
    public float startDepth;
    public boolean decreaseSpeed;
    public ClientEntity followEntity;
    public float followOffsetX, followOffsetY;

    //private float pox, poy; // Particle origin values
    public float pvx;
    public float pvy; // Particle velocity values
    public float spvx; // Particle velocity values
    public float spvy; // Particle velocity values
    public float coveredDistanceX, coveredDistanceY;
    public Interpolation velocityCurve;

    public ClientParticle() {
        visibleToRenderEngine = true;
    }

    @Override
    public void tick(float delta) {
        lifetime -= delta;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
        }

        if(fadeIn) {
            float d = lifetimeStatic - lifetime;

            if(d <= fadeInDuration) {
                useAlpha = d / fadeInDuration;
            } else {
                if(!fadeOut) {
                    useAlpha = 1.0f;
                } else if(lifetime > fadeOutDuration) {
                    useAlpha = 1.0f;
                }
            }
        }

        if(fadeOut) {
            if(lifetime <= fadeOutDuration) {
                useAlpha = (lifetime / fadeOutDuration);
                if(useAlpha < 0) useAlpha = 0;
            }
        }

        if(decreaseSpeed) {
            float progress = (lifetime / lifetimeStatic); // 1 -> 0
            float interpolated = Math.abs(Interpolation.pow3OutInverse.apply(Math.abs(progress - 1f)) - 1f);

            pvx = spvx * interpolated;
            pvy = spvy * interpolated;
        }

        if(velocityCurve != null) {
            float progress = (lifetime / lifetimeStatic); // 1 -> 0
            float interpolated = velocityCurve.apply(Math.abs(progress - 1f)); // 0 -> 1

            pvx = spvx * Math.abs(interpolated - 1f);
            pvy = spvy * Math.abs(interpolated - 1f);
        }

        float addY = pvy * delta;

        if(followEntity != null) {
            coveredDistanceX += pvx * delta;
            coveredDistanceY += addY;
            clientPosX = followEntity.finalTextureCenterX + coveredDistanceX + followOffsetX;
            clientPosY = followEntity.finalTextureCenterY + coveredDistanceY + followOffsetY;
        } else {
            clientPosX += pvx * delta;
            clientPosY += addY;
        }

        if(dynamicDepth) {
            if(followEntity != null) {
                depth = followEntity.depth - 0.01f;
            } else {
                depth += addY;
            }
        }

        if(rotationSpeed > 0) {
            rotation += delta * rotationSpeed;
        }
    }

    public void setParticleLifetime(float lifetime) {
        this.lifetime = lifetime;
        this.lifetimeStatic = lifetime;
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

    public void setVelocityCurve(Interpolation velocityCurve) {
        this.velocityCurve = velocityCurve;
    }

    public void setParticleFadeout(float fadeDuration) {
        fadeOut = true;
        this.fadeOutDuration = fadeDuration;
    }

    public void setParticleTextureRange(int start, int end) {
        particleRangeStart = start;
        particleRangeEnd = end;
    }

    public void setParticleFadein(float fadeDuration) {
        fadeIn = true;
        useAlpha = 0;
        this.fadeInDuration = fadeDuration;
    }

    public void setParticleConstantRotation(float speed) {
        rotationSpeed = speed;
    }

    public void setParticleDynamicDepth() {
        dynamicDepth = true;
        startDepth = depth;
    }

    public void setDecreaseSpeed() {
        decreaseSpeed = true;
    }

    public void setFollowEntity(ClientEntity followEntity) {
        this.followEntity = followEntity;
    }

    public void setFollowOffset(float followOffsetX, float followOffsetY) {
        this.followOffsetX = followOffsetX;
        this.followOffsetY = followOffsetY;
    }

}
