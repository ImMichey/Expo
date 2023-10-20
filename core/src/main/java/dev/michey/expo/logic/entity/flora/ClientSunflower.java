package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

public class ClientSunflower extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(
            0.2f, 0.5f, 0.02f, 0.03f, 0.03f, 0.04f, 2.0f, 5.0f, 0.5f, 1.5f
    );
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private Texture wheat;
    private TextureRegion wheatShadow;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        wheat = t("foliage/entity_sunflower/entity_sunflower.png");
        wheatShadow = new TextureRegion(t("foliage/entity_sunflower/entity_sunflower_sm.png"));

        updateTextureBounds(13, 30, 1, 1);
        interactionPointArray = generateInteractionArray(2, 12);

        contactAnimator.MIN_SQUISH = 0.6667f;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");
        contactAnimator.onContact();

        ParticleSheet.Common.spawnGrassHitParticles(this);
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
        setSelectionValues();

        rc.arraySpriteBatch.drawCustomVertices(wheat, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, wheat.getWidth(), wheat.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        rc.arraySpriteBatch.end();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawCustomVertices(wheat, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, wheat.getWidth(), wheat.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawCustomVertices(wheat, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, wheat.getWidth(), wheat.getHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] grassVertices = rc.arraySpriteBatch.obtainShadowVertices(wheatShadow, shadow);
        boolean drawGrass = rc.verticesInBounds(grassVertices);

        if(drawGrass) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradientCustomVertices(wheatShadow, wheatShadow.getRegionWidth(), wheatShadow.getRegionHeight() * contactAnimator.squish, shadow, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAOAuto50(rc);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SUNFLOWER;
    }

}