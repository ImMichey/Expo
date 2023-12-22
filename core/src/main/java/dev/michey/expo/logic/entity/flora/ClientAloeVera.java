package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientAloeVera extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private Texture grass;
    private TextureRegion grassShadow;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        contactAnimator.MIN_SQUISH = 0.6667f;
        grass = ExpoAssets.get().texture("foliage/entity_aloevera/entity_aloevera.png");
        grassShadow = new TextureRegion(t("foliage/entity_aloevera/entity_aloevera_sm.png"));

        updateTextureBounds(19, 14, 1, 1);
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
        contactAnimator.tick(delta);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues(Color.WHITE);

        rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, grass.getWidth(), grass.getHeight() * contactAnimator.squish, contactAnimator.value, contactAnimator.value);
        rc.arraySpriteBatch.end();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, grass.getWidth(), grass.getHeight() * contactAnimator.squish, contactAnimator.value, contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, grass.getWidth(), grass.getHeight() * contactAnimator.squish * -1, contactAnimator.value, contactAnimator.value);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] grassVertices = rc.arraySpriteBatch.obtainShadowVertices(grassShadow, shadow);
        boolean drawGrass = rc.verticesInBounds(grassVertices);

        if(drawGrass) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradientCustomVertices(grassShadow, grassShadow.getRegionWidth(), grassShadow.getRegionHeight() * contactAnimator.squish, shadow, contactAnimator.value, contactAnimator.value);
        }
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