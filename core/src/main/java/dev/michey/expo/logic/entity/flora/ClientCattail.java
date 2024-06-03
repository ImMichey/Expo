package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
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

public class ClientCattail extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(this,
            3.0f, 4.0f, 0.005f, 0.0075f, 0.009f, 0.010f, 2.0f, 5.0f, 0.5f, 1.5f);
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private int variant;
    private TextureRegion cattailTexture, cattailTexture_sel;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        contactAnimator.MIN_SQUISH = 0.75f;

        cattailTexture = new TextureRegion(tr("entity_cattail_" + variant));
        cattailTexture_sel = generateSelectionTexture(cattailTexture);

        if(MathUtils.randomBoolean()) {
            cattailTexture.flip(true, false);
            cattailTexture_sel.flip(true, false);
        }

        updateTextureBounds(cattailTexture);
        interactionPointArray = generateInteractionArray(4);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");
        contactAnimator.onContact();

        ParticleSheet.Common.spawnGrassHitParticles(this);
        if(newHealth <= 0) {
            ParticleSheet.Common.spawnDustHitParticles(this);
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

        if(visibleToRenderEngine) {
            foliageAnimator.resetWind();
            contactAnimator.tick(delta);
        }
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        setSelectionValues(Color.WHITE);

        rc.arraySpriteBatch.drawShiftedVertices(cattailTexture_sel, finalSelectionDrawPosX, finalSelectionDrawPosY + contactAnimator.squishAdjustment,
                cattailTexture_sel.getRegionWidth(), cattailTexture_sel.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this, 4);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.drawShiftedVertices(cattailTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                    cattailTexture.getRegionWidth(), cattailTexture.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawShiftedVertices(cattailTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                cattailTexture.getRegionWidth(), cattailTexture.getRegionHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(cattailTexture, foliageAnimator, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.4f, 0.5f, 0, 1);
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CATTAIL;
    }

}