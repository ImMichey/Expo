package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.devhud.DevHUD;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.CHUNK_SIZE;
import static dev.michey.expo.util.ExpoShared.PLAYER_CHUNK_VIEW_RANGE_ONE_DIR;

public class ClientGrass extends ClientEntity implements SelectableEntity {

    private Texture grass;
    private TextureRegion grassShadow;

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
        int variant = MathUtils.random(1, 5);
        grass = ExpoAssets.get().texture("foliage/entity_grass_" + variant + ".png");
        grassShadow = new TextureRegion(t("foliage/entity_grass_" + variant + "_shadow.png"));

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
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        log("onDamage");
        contactDelta = STEPS * 0.5f;
        contactDir = 1;//drawRootX < entity.drawRootX ? -1 : 1;
        AudioEngine.get().playSoundGroupManaged("leaves_rustle", new Vector2(drawRootX, drawRootY), CHUNK_SIZE, false);

        int particles = MathUtils.random(1, 1);

        for(int i = 0; i < particles; i++) {
            ClientParticleHit p = new ClientParticleHit();

            p.setParticleColor(MathUtils.randomBoolean() ? ClientStatic.COLOR_PARTICLE_GRASS_1 : ClientStatic.COLOR_PARTICLE_GRASS_2);
            p.setParticleLifetime(1.0f);
            p.setParticleOriginAndVelocity(drawCenterX + MathUtils.random(-1, 1), drawCenterY + MathUtils.random(-1, 1), MathUtils.random(-10, 10), MathUtils.random(-10, 10));
            p.setParticleRotation(MathUtils.random(360f));
            float scale = MathUtils.random(0.4f, 0.8f);
            p.setParticleScale(scale, scale);
            p.setParticleFadeout(0.25f);

            entityManager().addClientSideEntity(p);
        }
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(!windSwayAnimationActive() && drawnLastFrame) {
            for(List<ClientEntity> list : entityManager().getEntitiesByType(ClientEntityType.PLAYER, ClientEntityType.DUMMY)) {
                for(ClientEntity entity : list) {
                    //if(!entity.drawnLastFrame) continue;
                    if(!entity.isMoving()) continue;
                    float xDst = dstRootX(entity);
                    float yDst = dstRootY(entity);

                    if(xDst < 6.0f && yDst < 5.0f) {
                        // Contact.
                        contactDelta = STEPS * 0.5f;
                        contactDir = drawRootX < entity.drawRootX ? -1 : 1;
                        AudioEngine.get().playSoundGroupManaged("leaves_rustle", new Vector2(drawRootX, drawRootY), CHUNK_SIZE, false);
                    }
                }
            }
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        drawnLastFrame = rc.inDrawBounds(this);

        if(drawnLastFrame) {
            updateDepth(drawOffsetY);

            rc.arraySpriteBatch.setShader(rc.arrayWindShader);
            rc.useArrayBatch();
            drawGrass(rc, delta);
        }
    }

    private void drawGrass(RenderContext rc, float delta) {
        if(windSwayAnimationActive()) {
            contactDelta -= delta * SPEED;
            if(contactDelta < 0f) contactDelta = 0f;

            float full = STEPS * 0.5f;
            float diff = full - contactDelta;
            int decreases = (int) (diff / 0.5f);
            useStrength = STRENGTH - STRENGTH_DECREASE * decreases;

            verticesMovement = useStrength * contactDir * MathUtils.sin(((STEPS * 0.5f) - contactDelta) * MathUtils.PI2);
            rc.arraySpriteBatch.drawCustomVertices(grass, clientPosX, clientPosY, grass.getWidth(), grass.getHeight(), verticesMovement, verticesMovement);
        } else {
            rc.arraySpriteBatch.draw(grass, clientPosX, clientPosY);
        }
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.useArrayBatch();
        rc.arraySpriteBatch.setShader(rc.arrayWindShader);

        rc.arrayWindShader.bind();
        rc.arrayWindShader.setUniformi("u_selected", 1);
        rc.arrayWindShader.setUniformf("u_progress", entityManager().pulseProgress);

        drawGrass(rc, delta);

        rc.arraySpriteBatch.end();

        rc.arrayWindShader.bind();
        rc.arrayWindShader.setUniformi("u_selected", 0);

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawnShadowLastFrame = rc.inDrawBoundsShadow(this);

        if(drawnShadowLastFrame) {
            Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX + drawOffsetX, clientPosY + drawOffsetY + 1);
            rc.arraySpriteBatch.setShader(rc.arrayWindShader);
            rc.useArrayBatch();

            if(windSwayAnimationActive()) {
                rc.arraySpriteBatch.drawGradientCustomVertices(grassShadow, drawWidth, drawHeight, shadow, verticesMovement, verticesMovement);
            } else {
                rc.arraySpriteBatch.drawGradient(grassShadow, drawWidth, drawHeight, shadow);
            }
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.GRASS;
    }

    public boolean windSwayAnimationActive() {
        return contactDelta != 0f;
    }

}