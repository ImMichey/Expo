package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleColorMap;

public class ClientPoppy extends ClientEntity implements SelectableEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator();
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private Texture poppy;
    private TextureRegion shadow;
    private float[] interactionPointArray;

    private final float colorOffset = MathUtils.random(0.15f);

    @Override
    public void onCreation() {
        poppy = t("foliage/entity_poppy/entity_poppy.png");
        shadow = tr("entity_poppy_shadow_mask");
        updateTexture(0, 0, 8, 12);
        interactionPointArray = generateInteractionArray();
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        playEntitySound("grass_hit");
        contactAnimator.onContact();

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
            playEntitySound("harvest");
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        foliageAnimator.resetWind();
        contactAnimator.tick(delta);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
        rc.arraySpriteBatch.drawCustomVertices(poppy, clientPosX, clientPosY, poppy.getWidth(), poppy.getHeight(), foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(drawOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
            rc.arraySpriteBatch.drawCustomVertices(poppy, clientPosX, clientPosY, poppy.getWidth(), poppy.getHeight(), foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX + drawOffsetX, clientPosY + drawOffsetY);
        float[] grassVertices = rc.arraySpriteBatch.obtainShadowVertices(this.shadow, shadow);
        boolean drawGrass = rc.verticesInBounds(grassVertices);

        if(drawGrass) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradientCustomVertices(this.shadow, this.shadow.getRegionWidth(), this.shadow.getRegionHeight(), shadow, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.POPPY;
    }

}