package dev.michey.expo.logic.entity.flora;

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

public class ClientOakTreeSapling extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(this,
            0.5f, 1.0f, 0.02f, 0.03f, 0.03f, 0.04f, 2.0f, 5.0f, 0.5f, 1.5f
    );
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private Texture sapling;
    private TextureRegion saplingShadow;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        sapling = t("foliage/oak_tree_sapling.png");
        saplingShadow = tr("oak_tree_sapling");

        updateTextureBounds(12, 14, 1, 1);
        interactionPointArray = generateInteractionArray(2, 12);

        contactAnimator.MIN_SQUISH = 0.5f;
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
        setSelectionValues(Color.BLACK);
        rc.arraySpriteBatch.drawCustomVertices(sapling, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, sapling.getWidth(), sapling.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawCustomVertices(sapling, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, sapling.getWidth(), sapling.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawCustomVertices(sapling, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, sapling.getWidth(), sapling.getHeight() * contactAnimator.squish * -1, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] grassVertices = rc.arraySpriteBatch.obtainShadowVertices(saplingShadow, shadow);
        boolean drawGrass = rc.verticesInBounds(grassVertices);

        if(drawGrass) {
            rc.arraySpriteBatch.drawGradientCustomVertices(saplingShadow, saplingShadow.getRegionWidth(), saplingShadow.getRegionHeight() * contactAnimator.squish, shadow, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.OAK_TREE_SAPLING;
    }

}