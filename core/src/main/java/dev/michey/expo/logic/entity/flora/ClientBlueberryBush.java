package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;

public class ClientBlueberryBush extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private TextureRegion bushTexture, bushTexture_sel;
    private TextureRegion bushFruits, bushFruits_sel;
    private float[] interactionPointArray;

    private boolean hasBerries;

    @Override
    public void onCreation() {
        bushTexture = new TextureRegion(tr("entity_bbb"));
        bushFruits = new TextureRegion(tr("entity_bbb_fruits"));
        bushTexture_sel = generateSelectionTexture(bushTexture);
        bushFruits_sel = generateSelectionTexture(bushFruits);

        /*
        if(MathUtils.randomBoolean()) {
            bushTexture.flip(true, false);
            bushFruits.flip(true, false);
            bushTexture_sel.flip(true, false);
            bushFruits_sel.flip(true, false);
        }
        */

        updateTextureBounds(bushTexture);
        interactionPointArray = generateInteractionArray(3);

        contactAnimator.enableSquish = false;
        contactAnimator.STRENGTH = 2.0f;
        contactAnimator.STRENGTH_DECREASE = 0.4f;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        contactAnimator.onContact();
        playEntitySound("grass_hit");

        ParticleSheet.Common.spawnBlueberryHitParticles(this);
        if(newHealth <= 0) {
            ParticleSheet.Common.spawnDustHitParticles(this);
        }
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        contactAnimator.tick(delta);
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        TextureRegion use = hasBerries ? bushFruits_sel : bushTexture_sel;
        rc.arraySpriteBatch.drawShiftedVertices(use, finalSelectionDrawPosX, finalSelectionDrawPosY + contactAnimator.squishAdjustment,
                use.getRegionWidth(), use.getRegionHeight() * contactAnimator.squish, contactAnimator.value, 0);
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

            TextureRegion use = hasBerries ? bushFruits : bushTexture;
            rc.arraySpriteBatch.drawShiftedVertices(use, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                    use.getRegionWidth(), use.getRegionHeight() * contactAnimator.squish, contactAnimator.value, 0);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion use = hasBerries ? bushFruits : bushTexture;
        rc.arraySpriteBatch.drawShiftedVertices(use, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment,
                use.getRegionWidth(), use.getRegionHeight() * contactAnimator.squish * -1, contactAnimator.value, 0);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        TextureRegion use = hasBerries ? bushFruits : bushTexture;
        drawWindShadowIfVisible(use, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.5f, 0.5f, 0, 1.5f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BLUEBERRY_BUSH;
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        hasBerries = (boolean) payload[0];
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        hasBerries = (boolean) payload[0];

        if(!hasBerries) {
            contactAnimator.onContact();
        }
    }

}