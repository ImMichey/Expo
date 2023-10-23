package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientMaggot extends ClientEntity implements ReflectableEntity {

    private final ExpoAnimationHandler animationHandler;
    private boolean cachedMoving;
    private boolean flipped;

    private float damageDelta;
    private boolean damageTint;

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
        damageDelta = RenderContext.get().deltaTotal;
        damageTint = true;
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(isMoving()) {
            calculateReflection();
        }

        if(cachedMoving != isMoving()) {
            cachedMoving = !cachedMoving;
            animationHandler.reset();
            animationHandler.switchToAnimation(cachedMoving ? "walk" : "idle");
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY, f.getRegionWidth(), f.getRegionHeight() * -1);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        animationHandler.tick(delta);
        boolean flip = (!flipped && serverDirX == 0) || (flipped && serverDirX == 1);

        if(flip) {
            animationHandler.flipAllAnimations(true, false);
            flipped = !flipped;
        }

        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f);

        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            if(damageTint) {
                float MAX_TINT_DURATION = 0.2f;
                if(RenderContext.get().deltaTotal - damageDelta >= MAX_TINT_DURATION) damageTint = false;
            }

            if(damageTint) rc.arraySpriteBatch.setColor(ClientStatic.COLOR_DAMAGE_TINT);
            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY);
            if(damageTint) rc.arraySpriteBatch.setColor(Color.WHITE);
        }
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
