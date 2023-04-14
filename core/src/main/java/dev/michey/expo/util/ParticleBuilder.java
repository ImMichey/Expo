package dev.michey.expo.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;

public class ParticleBuilder {

    private ClientParticle[] particles;
    private final ClientEntityType type;

    public ParticleBuilder(ClientEntityType type) {
        this.type = type;
    }

    public ParticleBuilder amount(int amount) {
        particles = new ClientParticle[amount];
        for(int i = 0; i < amount; i++) {
            particles[i] = (ClientParticle) ClientEntityType.typeToClientEntity(type.ENTITY_ID);
        }
        return this;
    }

    public ParticleBuilder amount(int min, int max) {
        return amount(MathUtils.random(min, max));
    }

    public ParticleBuilder scale(float minScale, float maxScale) {
        for(ClientParticle p : particles) {
            float g = MathUtils.random(minScale, maxScale);
            p.setParticleScale(g, g);
        }
        return this;
    }

    public ParticleBuilder lifetime(float minLifetime, float maxLifetime) {
        for(ClientParticle p : particles) {
            float g = MathUtils.random(minLifetime, maxLifetime);
            p.setParticleLifetime(g);
        }
        return this;
    }

    public ParticleBuilder color(Color... colors) {
        for(ClientParticle p : particles) {
            p.setParticleColor(colors[MathUtils.random(0, colors.length - 1)]);
        }
        return this;
    }

    public ParticleBuilder velocity(float minX, float maxX, float minY, float maxY) {
        for(ClientParticle p : particles) {
            float x = MathUtils.random(minX, maxX);
            float y = MathUtils.random(minY, maxY);
            p.pvx = x;
            p.pvy = y;
        }
        return this;
    }

    public ParticleBuilder fadeout(float fadeout) {
        for(ClientParticle p : particles) {
            p.setParticleFadeout(fadeout);
        }
        return this;
    }

    public ParticleBuilder textureRange(int min, int max) {
        for(ClientParticle p : particles) {
            p.setParticleTextureRange(min, max);
        }
        return this;
    }

    public ParticleBuilder fadein(float fadein) {
        for(ClientParticle p : particles) {
            p.setParticleFadein(fadein);
        }
        return this;
    }

    public ParticleBuilder position(float x, float y) {
        for(ClientParticle p : particles) {
            p.clientPosX = x;
            p.clientPosY = y;
        }
        return this;
    }

    public ParticleBuilder offset(float xRange, float yRange) {
        for(ClientParticle p : particles) {
            p.clientPosX += MathUtils.random(xRange);
            p.clientPosY += MathUtils.random(yRange);
        }
        return this;
    }

    public ParticleBuilder randomRotation() {
        for(ClientParticle p : particles) {
            p.setParticleRotation(MathUtils.random(360f));
        }
        return this;
    }

    public ParticleBuilder rotateWithVelocity() {
        for(ClientParticle p : particles) {
            p.setParticleConstantRotation((Math.abs(p.pvx) + Math.abs(p.pvy)) * 0.5f / 24f * 360f);
        }
        return this;
    }

    public void spawn() {
        for(ClientParticle p : particles) {
            p.depth = p.clientPosY;
            ClientEntityManager.get().addClientSideEntity(p);
        }
    }

}
