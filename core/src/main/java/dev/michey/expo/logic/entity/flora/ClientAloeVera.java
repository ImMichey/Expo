package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

public class ClientAloeVera extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(this,
            3.0f, 4.0f, 0.005f, 0.0075f, 0.009f, 0.010f, 2.0f, 5.0f, 0.5f, 1.5f);
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private TextureRegion plantTexture, plantTexture_sel;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        contactAnimator.MIN_SQUISH = 0.6667f;
        plantTexture = new TextureRegion(tr("entity_aloevera"));
        plantTexture_sel = generateSelectionTexture(plantTexture);

        updateTextureBounds(plantTexture);
        interactionPointArray = generateInteractionArray(2);
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

        rc.arraySpriteBatch.drawShiftedVertices(plantTexture_sel, finalSelectionDrawPosX, finalSelectionDrawPosY + contactAnimator.squishAdjustment,
                plantTexture_sel.getRegionWidth(), plantTexture_sel.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this, 4);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.drawShiftedVertices(plantTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                    plantTexture.getRegionWidth(), plantTexture.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawShiftedVertices(plantTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                plantTexture.getRegionWidth(), plantTexture.getRegionHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(plantTexture, foliageAnimator, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAOAuto100(rc);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ALOE_VERA;
    }

}