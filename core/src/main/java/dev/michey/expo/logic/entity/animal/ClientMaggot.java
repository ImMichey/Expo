package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientMaggot extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private final ExpoAnimationHandler animationHandler;

    public ClientMaggot() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_maggot_idle", 3, 0.25f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_maggot_walk", 5, 0.15f));
    }

    @Override
    public void onCreation() {
        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("bloody_squish");
        }
    }

    @Override
    public boolean isMoving() {
        return serverMoveDistance > 0;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        setBlink();
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(isMoving()) {
            calculateReflection();
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY, f.getRegionWidth(), f.getRegionHeight() * -1);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f);

        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);

            updateDepth();
            rc.useArrayBatch();
            chooseArrayBatch(rc, interpolatedBlink);

            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY);

            rc.useRegularArrayShader();
            drawHealthBar(rc);
        }
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO50(rc, 0.25f, 0.25f, flipped ? 2 : -2, 0.5f);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(animationHandler.getActiveFrame());
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.MAGGOT;
    }

}
