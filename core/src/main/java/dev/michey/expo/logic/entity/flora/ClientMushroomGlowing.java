package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.ExpoLight;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleColorMap;

public class ClientMushroomGlowing extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private TextureRegion texture;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;

    private ExpoLight mushroomLight;

    @Override
    public void onCreation() {
        texture = tr("entity_mushroom_glowing");
        selectionTexture = generateSelectionTexture(texture);
        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray();

        mushroomLight = new ExpoLight(64.0f, 32, 0.75f, 0.75f);
        //mushroomLight.color(0.0f, 0.75f, 1.0f, 1.0f);
        mushroomLight.color(1.0f, 0.6f, 0.0f, 1.0f);
        mushroomLight.setPulsating(1.0f, 128.0f, 144.0f);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");

        int particles = MathUtils.random(2, 5);

        for(int i = 0; i < particles; i++) {
            ClientParticleHit p = new ClientParticleHit();

            float velocityX = MathUtils.random(-24, 24);
            float velocityY = MathUtils.random(-24, 24);

            p.setParticleColor(MathUtils.randomBoolean() ? ParticleColorMap.COLOR_PARTICLE_MUSHROOM_1 : ParticleColorMap.COLOR_PARTICLE_MUSHROOM_2);
            p.setParticleLifetime(0.3f);
            p.setParticleOriginAndVelocity(finalTextureCenterX, finalTextureCenterY, velocityX, velocityY);
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
        mushroomLight.delete();

        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("harvest");
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        mushroomLight.update(clientPosX, clientPosY + 4.5f, delta);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(texture);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.MUSHROOM_GLOWING;
    }

}