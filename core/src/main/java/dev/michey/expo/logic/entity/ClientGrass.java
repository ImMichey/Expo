package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleColorMap;

import java.util.List;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class ClientGrass extends ClientEntity implements SelectableEntity {

    private int variant;
    private Texture grass;
    private TextureRegion grassShadow;
    private float[] interactionPointArray;

    private final float u_speed = MathUtils.random(0.5f, 1.2f);
    private final float u_offset = MathUtils.random(100f);
    private final float u_minStrength = MathUtils.random(0.02f, 0.04f);
    private final float u_maxStrength = MathUtils.random(0.04f, 0.06f);
    private final float u_interval = MathUtils.random(2.0f, 5.0f);
    private final float u_detail = MathUtils.random(0.5f, 1.5f);

    private float wind;
    private boolean calculatedWindThisTick = false;

    private final float colorOffset = MathUtils.random(0.15f);

    /** Wind sway vertices animation */
    private float contactDelta = 0f;
    private float contactDir = 0f;
    private final float SPEED = 3.5f;
    private final float STRENGTH = 3.5f;
    private final float STRENGTH_DECREASE = 0.7f;
    private final int STEPS = 5; // step = 0.5f (half radiant)
    private float useStrength = STRENGTH;
    private float verticesMovement;

    @Override
    public void onCreation() {
        grass = ExpoAssets.get().texture("foliage/entity_grass/entity_grass_" + variant + ".png");
        grassShadow = new TextureRegion(t("foliage/entity_grass/entity_grass_" + variant + "_shadow.png"));

        float x = 0, y = 0, w = 0, h = 0;

        if(variant == 1) {
            x = 2;
            y = 3;
            w = 11;
            h = 9;
        } else if(variant == 2) {
            x = 2;
            y = 3;
            w = 12;
            h = 10;
        } else if(variant == 3) {
            x = 2;
            y = 4;
            w = 13;
            h = 8;
        } else if(variant == 4) {
            x = 1;
            y = 3;
            w = 13;
            h = 10;
        } else if(variant == 5) {
            x = 2;
            y = 2;
            w = 13;
            h = 12;
        }

        updateTexture(x, y, w, h);
        interactionPointArray = new float[] {
                (clientPosX + drawOffsetX + 3), (clientPosY + drawOffsetY + 3),
                (clientPosX + drawOffsetX - 3 + drawWidth), (clientPosY + drawOffsetY + 3),
                (clientPosX + drawOffsetX - 3 + drawWidth), (clientPosY + drawOffsetY - 3 + drawHeight),
                (clientPosX + drawOffsetX + 3), (clientPosY + drawOffsetY - 3 + drawHeight),
        };
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        AudioEngine.get().playSoundGroupManaged("grass_hit", new Vector2(drawRootX, drawRootY), PLAYER_AUDIO_RANGE, false);
        contactDelta = STEPS * 0.5f;
        contactDir = 1;

        int particles = MathUtils.random(4, 7);

        for(int i = 0; i < particles; i++) {
            ClientParticleHit p = new ClientParticleHit();

            float velocityX = MathUtils.random(-24, 24);
            float velocityY = MathUtils.random(-24, 24);

            p.setParticleTextureRange(3, 7);
            p.setParticleColor(MathUtils.randomBoolean() ? ParticleColorMap.COLOR_PARTICLE_GRASS_1 : ParticleColorMap.COLOR_PARTICLE_GRASS_2);
            p.setParticleLifetime(0.3f);
            p.setParticleOriginAndVelocity(drawCenterX, drawCenterY, velocityX, velocityY);
            float scale = MathUtils.random(0.6f, 0.9f);
            p.setParticleScale(scale, scale);
            p.setParticleFadeout(0.1f);
            p.setParticleRotation(MathUtils.random(360f));
            p.setParticleConstantRotation((Math.abs(velocityX) + Math.abs(velocityY)) * 0.5f / 24f * 360f);
            p.depth = depth - 0.0001f;

            entityManager().addClientSideEntity(p);
        }
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            AudioEngine.get().playSoundGroupManaged("harvest", new Vector2(drawRootX, drawRootY), PLAYER_AUDIO_RANGE, false);
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        calculatedWindThisTick = false;

        if(contactDelta == 0 && visibleToRenderEngine) {
            for(List<ClientEntity> list : entityManager().getEntitiesByType(ClientEntityType.PLAYER, ClientEntityType.DUMMY)) {
                for(ClientEntity entity : list) {
                    //if(!entity.drawnLastFrame) continue;
                    if(!entity.isMoving()) continue;
                    float xDst = dstRootX(entity);
                    float yDst = dstRootY(entity);

                    if(xDst < 6.0f && yDst < 5.0f) {
                        // Contact.
                        AudioEngine.get().playSoundGroupManaged("leaves_rustle", new Vector2(drawRootX, drawRootY), PLAYER_AUDIO_RANGE, false);
                        contactDelta = STEPS * 0.5f;
                        contactDir = entity.serverDirX == 0 ? (entity.drawRootX < drawRootX ? 1 : -1) : (entity.serverDirX < 0 ? -1 : 1);
                    }
                }
            }
        }

        if(contactDelta != 0) {
            contactDelta -= delta * SPEED;
            if(contactDelta < 0f) contactDelta = 0f;

            float full = STEPS * 0.5f;
            float diff = full - contactDelta;
            int decreases = (int) (diff / 0.5f);
            useStrength = STRENGTH - STRENGTH_DECREASE * decreases;

            verticesMovement = useStrength * contactDir * MathUtils.sin(((STEPS * 0.5f) - contactDelta) * MathUtils.PI2);
        } else {
            verticesMovement = 0;
        }
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        calculateWindOnDemand(rc.deltaTotal);
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
        rc.arraySpriteBatch.drawCustomVertices(grass, clientPosX, clientPosY, grass.getWidth(), grass.getHeight(), wind + verticesMovement, wind + verticesMovement);
        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            calculateWindOnDemand(rc.deltaTotal);
            updateDepth(drawOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
            rc.arraySpriteBatch.drawCustomVertices(grass, clientPosX, clientPosY, grass.getWidth(), grass.getHeight(), wind + verticesMovement, wind + verticesMovement);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    private void calculateWindOnDemand(float deltaTotal) {
        if(!calculatedWindThisTick) {
            calculatedWindThisTick = true;
            wind = ShadowUtils.getWind(u_maxStrength, u_minStrength, deltaTotal * u_speed + u_offset, u_interval, u_detail);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        calculateWindOnDemand(rc.deltaTotal);
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX + drawOffsetX, clientPosY + drawOffsetY);
        float[] grassVertices = rc.arraySpriteBatch.obtainShadowVertices(grassShadow, shadow);
        boolean drawGrass = rc.verticesInBounds(grassVertices);

        if(drawGrass) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradientCustomVertices(grassShadow, grassShadow.getRegionWidth(), grassShadow.getRegionHeight(), shadow, wind + verticesMovement, wind + verticesMovement);
        }
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.GRASS;
    }

}