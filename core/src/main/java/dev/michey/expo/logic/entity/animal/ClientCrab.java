package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientCrab extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private final ExpoAnimationHandler animationHandler;

    private boolean cachedMoving;
    private boolean flipped;

    private int lastFootstepIndex;

    public ClientCrab() {
        animationHandler = new ExpoAnimationHandler() {
            @Override
            public void onAnimationFinish() {
                lastFootstepIndex = 0;
            }
        };
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_crab_idle", 4, 0.25f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_crab_walk", 12, 0.05f));
    }

    @Override
    public void onCreation() {
        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        blinkDelta = 1.0f;
        ParticleSheet.Common.spawnBloodParticles(this, 0, -1.5f);

        ClientEntity existing = entityManager().getEntityById(damageSourceEntityId);
        Vector2 dir = existing == null ? null : new Vector2(existing.clientPosX, existing.clientPosY).sub(clientPosX, clientPosY).nor();

        spawnDamageIndicator((int) damage, clientPosX, clientPosY + 8, dir);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("bloody_squish");
        }
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
    public boolean isMoving() {
        return serverMoveDistance > 0;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta, 7.5f);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);
            boolean flip = (!flipped && serverDirX == 0) || (flipped && serverDirX == 1);

            if(flip) {
                animationHandler.flipAllAnimations(true, false);
                flipped = !flipped;
            }

            int i = animationHandler.getActiveAnimation().getFrameIndex();

            if((i == 4 || i == 10) && (lastFootstepIndex != i)) {
                lastFootstepIndex = i;

                playEntitySound(getFootstepSound(), 0.3f);
            }

            TextureRegion cf = animationHandler.getActiveFrame();
            updateTextureBounds(cf);

            updateDepth();
            rc.useArrayBatch();
            chooseArrayBatch(rc, interpolatedBlink);

            rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(animationHandler.getActiveFrame());
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.333f, 0.333f, 0, 1);
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion cf = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY, cf.getRegionWidth(), cf.getRegionHeight() * -1);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CRAB;
    }

}
