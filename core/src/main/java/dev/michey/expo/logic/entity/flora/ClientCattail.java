package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ExpoAssets;
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

public class ClientCattail extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(0.8f, 1.7f, 0.02f, 0.03f, 0.035f, 0.05f, 2.0f, 5.0f, 0.5f, 1.5f);
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private int variant;
    private Texture grass;
    private TextureRegion grassShadow;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        contactAnimator.MIN_SQUISH = 0.6667f;
        grass = ExpoAssets.get().texture("foliage/entity_cattail/cattail_variation_" + variant + ".png");
        grassShadow = tr("cattail_variation_shadow_" + variant);

        float w = 0, h = 0;

        if(variant == 1) {
            w = 15;
            h = 29;
        } else if(variant == 2) {
            w = 19;
            h = 37;
        } else if(variant == 3) {
            w = 16;
            h = 42;
        }

        updateTextureBounds(w, h, 1, 1);
        interactionPointArray = generateInteractionArray(2, textureHeight - 12);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");
        contactAnimator.onContact();

        new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                .amount(4, 7)
                .scale(0.6f, 0.9f)
                .lifetime(0.3f, 0.35f)
                .color(ParticleColorMap.random(1))
                .position(finalTextureCenterX, finalTextureCenterY)
                .velocity(-24, 24, -24, 24)
                .fadeout(0.10f)
                .textureRange(3, 7)
                .randomRotation()
                .rotateWithVelocity()
                .depth(depth - 0.0001f)
                .spawn();
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
        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, Color.BLACK, false);

        rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, grass.getWidth(), grass.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
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

            rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, grass.getWidth(), grass.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, grass.getWidth(), grass.getHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] grassVertices = rc.arraySpriteBatch.obtainShadowVertices(grassShadow, shadow);
        boolean drawGrass = rc.verticesInBounds(grassVertices);

        if(drawGrass) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradientCustomVertices(grassShadow, grassShadow.getRegionWidth(), grassShadow.getRegionHeight() * contactAnimator.squish, shadow, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CATTAIL;
    }

}