package dev.michey.expo.logic.entity.crop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

public class ClientCropWheat extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(this,
            0.5f, 1.0f, 0.02f, 0.03f, 0.03f, 0.04f, 2.0f, 5.0f, 0.5f, 1.5f
    );
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    public int cropAge;
    public Texture cropTexture;
    public TextureRegion wheatShadow;

    public float[] interactionPointArray;

    public ClientCropWheat() {
        contactAnimator.MIN_SQUISH = 0.5f;
    }

    @Override
    public void onCreation() {
        updateCropTexture();
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");
        contactAnimator.onContact();

        new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                .amount(5, 9)
                .scale(0.7f, 1.0f)
                .lifetime(0.3f, 0.35f)
                .color(ParticleColorMap.of(4))
                .position(finalTextureCenterX, finalTextureCenterY)
                .velocity(-24, 24, -24, 24)
                .fadeout(0.10f)
                .textureRange(3, 7)
                .randomRotation()
                .rotateWithVelocity()
                .depth(depth - 0.0001f)
                .spawn();
    }

    public void updateCropTexture() {
        cropTexture = t("foliage/entity_wheat/entity_crop_wheat_stage_" + (cropAge + 1) + ".png");
        wheatShadow = tr("entity_crop_wheat_stage_sm_" + (cropAge + 1));

        // 17,10
        float[][] b = new float[][] {
                new float[] {17, 5},
                new float[] {17, 6},
                new float[] {17, 8},
                new float[] {17, 10},
                new float[] {17, 17},
                new float[] {17, 24}
        };
        updateTextureBounds(b[cropAge][0], b[cropAge][1], 1, 1);

        interactionPointArray = new float[] {finalTextureStartX + textureWidth * 0.5f, finalTextureStartY};
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
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawCustomVertices(cropTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, cropTexture.getWidth(), cropTexture.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawCustomVertices(cropTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, cropTexture.getWidth(), cropTexture.getHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
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
    public ClientEntityType getEntityType() {
        return ClientEntityType.CROP_WHEAT;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        setSelectionValues(Color.BLACK);

        rc.arraySpriteBatch.drawCustomVertices(cropTexture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, cropTexture.getWidth(), cropTexture.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        rc.arraySpriteBatch.end();
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        cropAge = (int) payload[0];
        updateCropTexture();
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        cropAge = (int) payload[0];
        updateCropTexture();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

}
