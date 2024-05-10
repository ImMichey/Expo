package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientSunflower extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(this);
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private int variant;
    private TextureRegion sunflowerTexture, sunflowerTexture_sel;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        sunflowerTexture = new TextureRegion(tr("entity_sunflower_" + variant));
        sunflowerTexture_sel = generateSelectionTexture(sunflowerTexture);

        if(MathUtils.randomBoolean()) {
            sunflowerTexture.flip(true, false);
            sunflowerTexture_sel.flip(true, false);
        }

        updateTextureBounds(sunflowerTexture);
        interactionPointArray = generateInteractionArray(2, 12);

        contactAnimator.MIN_SQUISH = 0.75f;
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
        rc.arraySpriteBatch.drawShiftedVertices(sunflowerTexture_sel, finalSelectionDrawPosX, finalSelectionDrawPosY + contactAnimator.squishAdjustment,
                sunflowerTexture_sel.getRegionWidth(), sunflowerTexture_sel.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this, 4);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.drawShiftedVertices(sunflowerTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                    sunflowerTexture.getRegionWidth(), sunflowerTexture.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawShiftedVertices(sunflowerTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                sunflowerTexture.getRegionWidth(), sunflowerTexture.getRegionHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(sunflowerTexture, foliageAnimator, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAOAuto100(rc);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SUNFLOWER;
    }

}