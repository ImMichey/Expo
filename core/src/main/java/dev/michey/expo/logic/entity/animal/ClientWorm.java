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

public class ClientWorm extends ClientEntity implements ReflectableEntity {

    private final ExpoAnimationHandler animationHandler;
    private boolean cachedMoving;
    private boolean flipped;

    public ClientWorm() {
        animationHandler = new ExpoAnimationHandler(this) {
            @Override
            public void onAnimationFinish() {
                if(isInWater()) spawnPuddle(false, flipped ? 2.5f : -2.5f, 0);
            }
        };
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_wormS_idle", 3, 0.25f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_wormS_walk", 5, 0.125f));
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
        blinkDelta = 1.0f;
        /*
        ClientEntity existing = entityManager().getEntityById(damageSourceEntityId);
        Vector2 dir = existing == null ? null : new Vector2(existing.clientPosX, existing.clientPosY).sub(clientPosX, clientPosY).nor();

        spawnDamageIndicator((int) damage, clientPosX, clientPosY + 8, dir);
        */
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
        float interpolatedBlink = tickBlink(delta);
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
            chooseArrayBatch(rc, interpolatedBlink);

            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(animationHandler.getActiveFrame());
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.WORM;
    }

}
