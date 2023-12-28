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

import static dev.michey.expo.render.RenderContext.TRANS_100_PACKED;

public class ClientGrass extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(this);
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private int variant;
    private TextureRegion grassTexture, grassTexture_sel;
    private float[] interactionPointArray;

    private final float colorOffset = MathUtils.random(0.15f);

    @Override
    public void onCreation() {
        grassTexture = new TextureRegion(tr("entity_grass_redo_" + variant));
        grassTexture_sel = generateSelectionTexture(grassTexture);

        if(MathUtils.randomBoolean()) {
            grassTexture.flip(true, false);
            grassTexture_sel.flip(true, false);
        }

        updateTextureBounds(grassTexture);
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
        setSelectionValues(Color.BLACK);

        rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
        rc.arraySpriteBatch.drawShiftedVertices(grassTexture_sel, finalSelectionDrawPosX, finalSelectionDrawPosY + contactAnimator.squishAdjustment,
                grassTexture_sel.getRegionWidth(), grassTexture_sel.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this, 4);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
            rc.arraySpriteBatch.drawShiftedVertices(grassTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                    grassTexture.getRegionWidth(), grassTexture.getRegionHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, 0);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawShiftedVertices(grassTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                grassTexture.getRegionWidth(), grassTexture.getRegionHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, 0);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(grassTexture, foliageAnimator, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        float scale = textureWidth / rc.aoTexture.getWidth();
        if(rc.aoBatch.getPackedColor() != TRANS_100_PACKED) rc.aoBatch.setPackedColor(TRANS_100_PACKED);
        drawAO(rc, scale, scale, 0, 0.5f);
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.GRASS;
    }

}