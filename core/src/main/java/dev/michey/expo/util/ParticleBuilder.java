package dev.michey.expo.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.ClientParticle;
import dev.michey.expo.noise.TileLayerType;

import java.util.List;

public class ParticleBuilder {

    private final ClientEntityType type;

    // Builder values are cached so particle emitters can re-use those values
    private int amountMin, amountMax;
    private float scaleMin, scaleMax;
    private float lifetimeMin, lifetimeMax;
    private Color[] colors;
    private float velocityMinX, velocityMaxX;
    private float velocityMinY, velocityMaxY;
    private float velocityDirectional;
    private Interpolation velocityCurve;
    private float fadeout;
    private float fadein;
    private float fadeoutPercentage;
    private int textureMin, textureMax;
    private float startPosX, startPosY;
    private float offsetX, offsetY;
    private float followOffsetX, followOffsetY;
    private boolean randomRotation;
    private boolean rotateWithVelocity;
    private boolean decreaseSpeed;
    private ClientEntity followEntity;
    private float depth = Float.MAX_VALUE;
    private boolean dynamicDepth;

    public ParticleBuilder(ClientEntityType type) {
        this.type = type;
    }

    public ParticleBuilder amount(int amountTotal) {
        return amount(amountTotal, amountTotal);
    }

    public ParticleBuilder amount(int min, int max) {
        amountMin = min;
        amountMax = max;
        return this;
    }

    public ParticleBuilder scale(float scaleMin, float scaleMax) {
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        return this;
    }

    public ParticleBuilder lifetime(float lifetimeMin, float lifetimeMax) {
        this.lifetimeMin = lifetimeMin;
        this.lifetimeMax = lifetimeMax;
        return this;
    }

    public ParticleBuilder fadeoutLifetime(float percentage) {
        this.fadeoutPercentage = percentage;
        return this;
    }

    public ParticleBuilder color(TileLayerType tileLayerType) {
        if(tileLayerType == TileLayerType.FOREST) {
            color(ParticleColorMap.of(1));
        }
        return this;
    }

    public ParticleBuilder color(Color... colors) {
        this.colors = colors;
        return this;
    }

    public ParticleBuilder color(List<Color> colors) {
        this.colors = new Color[colors.size()];
        for(int i = 0; i < colors.size(); i++) this.colors[i] = colors.get(i);
        return this;
    }

    public ParticleBuilder velocity(float velocityMinX, float velocityMaxX, float velocityMinY, float velocityMaxY) {
        this.velocityMinX = velocityMinX;
        this.velocityMaxX = velocityMaxX;
        this.velocityMinY = velocityMinY;
        this.velocityMaxY = velocityMaxY;
        return this;
    }

    public ParticleBuilder velocityCurve(Interpolation interpolation) {
        this.velocityCurve = interpolation;
        return this;
    }

    public ParticleBuilder velocityDirectional(float max) {
        this.velocityDirectional = max;
        return this;
    }

    public ParticleBuilder velocityNormalized(float max) {
        float x = MathUtils.cosDeg(MathUtils.random(360f));
        float y = MathUtils.sinDeg(MathUtils.random(360f));
        Vector2 vv = new Vector2(x, y).nor().scl(max);

        this.velocityMinX = -vv.x;
        this.velocityMaxX = vv.x;

        this.velocityMinY = -vv.y;
        this.velocityMaxY = vv.y;

        return this;
    }

    public ParticleBuilder fadeout(float fadeout) {
        this.fadeout = fadeout;
        return this;
    }

    public ParticleBuilder textureRange(int textureMin, int textureMax) {
        this.textureMin = textureMin;
        this.textureMax = textureMax;
        return this;
    }

    public ParticleBuilder fadein(float fadein) {
        this.fadein = fadein;
        return this;
    }

    public ParticleBuilder position(float startPosX, float startPosY) {
        this.startPosX = startPosX;
        this.startPosY = startPosY;
        return this;
    }

    public ParticleBuilder offset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        return this;
    }

    public ParticleBuilder randomRotation() {
        randomRotation = true;
        return this;
    }

    public ParticleBuilder rotateWithVelocity() {
        rotateWithVelocity = true;
        return this;
    }

    public ParticleBuilder decreaseSpeed() {
        decreaseSpeed = true;
        return this;
    }

    public ParticleBuilder followEntity(ClientEntity followEntity) {
        this.followEntity = followEntity;
        return this;
    }

    public ParticleBuilder followOffset(float offsetX, float offsetY) {
        this.followOffsetX = offsetX;
        this.followOffsetY = offsetY;
        return this;
    }

    public ParticleBuilder depth(float depth) {
        this.depth = depth;
        return this;
    }

    public ParticleBuilder dynamicDepth() {
        dynamicDepth = true;
        return this;
    }

    public void spawn() {
        int spawn = MathUtils.random(amountMin, amountMax);

        for(int i = 0; i < spawn; i++) {
            ClientParticle p = (ClientParticle) ClientEntityType.typeToClientEntity(type.ENTITY_ID);

            float scale = MathUtils.random(scaleMin, scaleMax);
            p.setParticleScale(scale, scale);

            float lifetime = MathUtils.random(lifetimeMin, lifetimeMax);
            p.setParticleLifetime(lifetime);

            if(colors != null) {
                p.setParticleColor(colors[MathUtils.random(0, colors.length - 1)]);
            }

            float velocityX;
            float velocityY;

            if(velocityDirectional > 0) {
                float angle = MathUtils.random(360f);
                float x = MathUtils.cosDeg(angle);
                float y = MathUtils.sinDeg(angle);
                Vector2 vec = new Vector2(x, y).nor().scl(velocityDirectional);
                velocityX = vec.x;
                velocityY = vec.y;
            } else {
                velocityX = MathUtils.random(velocityMinX, velocityMaxX);
                velocityY = MathUtils.random(velocityMinY, velocityMaxY);
            }

            p.pvx = velocityX;
            p.pvy = velocityY;
            p.spvx = velocityX;
            p.spvy = velocityY;

            p.setParticleFadeout(fadeoutPercentage > 0 ? (lifetime * fadeoutPercentage) : fadeout);
            p.setParticleFadein(fadein);

            p.setParticleTextureRange(textureMin, textureMax);

            p.clientPosX = startPosX;
            p.clientPosY = startPosY;
            p.clientPosX += MathUtils.random(offsetX);
            p.clientPosY += MathUtils.random(offsetY);
            p.depth = depth == Float.MAX_VALUE ? p.clientPosY : depth;

            if(randomRotation) p.setParticleRotation(MathUtils.random(360f));
            if(rotateWithVelocity) p.setParticleConstantRotation((Math.abs(p.pvx) + Math.abs(p.pvy)) * 0.5f / 24f * 360f);
            if(dynamicDepth) p.setParticleDynamicDepth();
            if(decreaseSpeed) p.setDecreaseSpeed();
            if(followEntity != null) {
                p.setFollowEntity(followEntity);
                p.setFollowOffset(followOffsetX, followOffsetY);
            }
            if(velocityCurve != null) {
                p.setVelocityCurve(velocityCurve);
            }

            ClientEntityManager.get().addClientSideEntity(p);
        }
    }

}
