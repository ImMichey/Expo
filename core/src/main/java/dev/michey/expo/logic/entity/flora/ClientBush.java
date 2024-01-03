package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientBush extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private TextureRegion bushTexture, bushTexture_sel;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        bushTexture = new TextureRegion(tr("entity_bush"));
        bushTexture_sel = generateSelectionTexture(bushTexture);

        if(MathUtils.randomBoolean()) {
            bushTexture.flip(true, false);
            bushTexture_sel.flip(true, false);
        }

        updateTextureBounds(bushTexture);
        interactionPointArray = generateInteractionArray(3);

        contactAnimator.enableSquish = false;
        contactAnimator.STRENGTH = 2.33f;
        contactAnimator.STRENGTH_DECREASE = 0.5f;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        contactAnimator.onContact();
        playEntitySound("grass_hit");

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
        contactAnimator.tick(delta);
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        rc.arraySpriteBatch.drawShiftedVertices(bushTexture_sel, finalSelectionDrawPosX, finalSelectionDrawPosY + contactAnimator.squishAdjustment,
                bushTexture_sel.getRegionWidth(), bushTexture_sel.getRegionHeight() * contactAnimator.squish, contactAnimator.value, 0);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.drawShiftedVertices(bushTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                    bushTexture.getRegionWidth(), bushTexture.getRegionHeight() * contactAnimator.squish, contactAnimator.value, 0);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawShiftedVertices(bushTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                bushTexture.getRegionWidth(), bushTexture.getRegionHeight() * contactAnimator.squish * -1, contactAnimator.value, 0);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(bushTexture, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.4f, 0.4f, 0, 1.5f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BUSH;
    }

}