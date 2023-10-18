package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

public class ClientBlueberryBush extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private TextureRegion mask;
    private Texture bush;
    private Texture bushFruits;
    private float[] interactionPointArray;

    private boolean hasBerries;

    @Override
    public void onCreation() {
        mask = tr("ebbbmask");
        bush = t("foliage/entity_blueberrybush/ebbb0.png");
        bushFruits = t("foliage/entity_blueberrybush/ebbb1.png");

        updateTextureBounds(16, 16, 1, 1);
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
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        Texture useTex = hasBerries ? bushFruits : bush;
        rc.arraySpriteBatch.drawCustomVertices(useTex, finalDrawPosX, finalDrawPosY, useTex.getWidth(), useTex.getHeight(), contactAnimator.value, contactAnimator.value);
        rc.arraySpriteBatch.end();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY + 1);
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            Texture useTex = hasBerries ? bushFruits : bush;
            rc.arraySpriteBatch.drawCustomVertices(useTex, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, useTex.getWidth(), useTex.getHeight() * contactAnimator.squish, contactAnimator.value, contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        Texture useTex = hasBerries ? bushFruits : bush;
        rc.arraySpriteBatch.drawCustomVertices(useTex, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, useTex.getWidth(), useTex.getHeight() * contactAnimator.squish * -1, contactAnimator.value, contactAnimator.value);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(mask, contactAnimator);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.4f, 0.4f, 0, 1.5f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BLUEBERRY_BUSH;
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        hasBerries = (boolean) payload[0];
    }

    @Override
    public void readEntityDataUpdate(Object[] payload) {
        hasBerries = (boolean) payload[0];

        if(!hasBerries) {
            contactAnimator.onContact();
        }
    }

}