package dev.michey.expo.logic.entity.flora;

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

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class ClientMushroomBrown extends ClientEntity implements SelectableEntity {

    private TextureRegion texture;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        texture = ExpoAssets.get().textureRegion("mushroom_brown_var2");
        updateTexture(texture);
        interactionPointArray = generateInteractionArray();
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        playEntitySound("grass_hit");

        int particles = MathUtils.random(2, 5);

        for(int i = 0; i < particles; i++) {
            ClientParticleHit p = new ClientParticleHit();

            float velocityX = MathUtils.random(-24, 24);
            float velocityY = MathUtils.random(-24, 24);

            p.setParticleColor(MathUtils.randomBoolean() ? ParticleColorMap.COLOR_PARTICLE_MUSHROOM_1 : ParticleColorMap.COLOR_PARTICLE_MUSHROOM_2);
            p.setParticleLifetime(0.3f);
            p.setParticleOriginAndVelocity(drawCenterX, drawCenterY, velocityX, velocityY);
            float scale = MathUtils.random(0.5f, 1.0f);
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
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.draw(texture, clientPosX + drawOffsetX, clientPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(drawOffsetY);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, clientPosX + drawOffsetX, clientPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX + drawOffsetX, clientPosY);
        float[] mushroomVertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);
        boolean drawMushroom = rc.verticesInBounds(mushroomVertices);

        if(drawMushroom) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(texture, drawWidth, drawHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.MUSHROOM_BROWN;
    }

}