package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientGrass extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator();
    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private int variant;
    private Texture grass;
    private TextureRegion grassShadow;
    private float[] interactionPointArray;

    private final float colorOffset = MathUtils.random(0.15f);

    @Override
    public void onCreation() {
        grass = ExpoAssets.get().texture("foliage/entity_grass/entity_grass_" + variant + ".png");
        grassShadow = new TextureRegion(t("foliage/entity_grass/entity_grass_" + variant + "_shadow.png"));

        float w = 0, h = 0;

        if(variant == 1) {
            w = 11;
            h = 9;
        } else if(variant == 2) {
            w = 12;
            h = 10;
        } else if(variant == 3) {
            w = 13;
            h = 8;
        } else if(variant == 4) {
            w = 13;
            h = 10;
        } else if(variant == 5) {
            w = 11;
            h = 6;
        } else if(variant == 6) {
            w = 12;
            h = 10;
        } else if(variant == 7) {
            w = 19;
            h = 9;
        } else if(variant == 8) {
            w = 20;
            h = 12;
        }

        updateTextureBounds(w, h, 1, 1);
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

        rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
        rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, grass.getWidth(), grass.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            foliageAnimator.calculateWindOnDemand();
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.setColor(1.0f - colorOffset, 1.0f, 1.0f - colorOffset, 1.0f);
            rc.arraySpriteBatch.drawCustomVertices(grass, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, grass.getWidth(), grass.getHeight() * contactAnimator.squish, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
            rc.arraySpriteBatch.setColor(Color.WHITE);
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
    public void renderAO(RenderContext rc) {
        drawAOAuto100(rc);
        //drawAOAuto33(rc);
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