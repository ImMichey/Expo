package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

public class ClientBush extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private final ContactAnimator contactAnimator = new ContactAnimator(this);

    private Texture texture;
    private TextureRegion shadowMask;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        texture = t("foliage/entity_bush/entity_bush.png");
        shadowMask = tr("entity_bush_sm");
        updateTextureBounds(18, 17, 1, 1);
        interactionPointArray = generateInteractionArray(3);
        contactAnimator.enableSquish = false;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        contactAnimator.onContact();
        playEntitySound("grass_hit");

        new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                .amount(4, 7)
                .scale(0.6f, 0.9f)
                .lifetime(0.3f, 0.35f)
                .color(ParticleColorMap.of(1))
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
        contactAnimator.tick(delta);
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.drawCustomVertices(texture, finalDrawPosX, finalDrawPosY, texture.getWidth(), texture.getHeight(), contactAnimator.value, contactAnimator.value);
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
            updateDepth(textureOffsetY);

            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawCustomVertices(texture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment, texture.getWidth(), texture.getHeight() * contactAnimator.squish, contactAnimator.value, contactAnimator.value);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.drawCustomVertices(texture, finalDrawPosX, finalDrawPosY + contactAnimator.squishAdjustment + 2, texture.getWidth(), texture.getHeight() * contactAnimator.squish * -1, contactAnimator.value, contactAnimator.value);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawWindShadowIfVisible(shadowMask, contactAnimator);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BUSH;
    }

}