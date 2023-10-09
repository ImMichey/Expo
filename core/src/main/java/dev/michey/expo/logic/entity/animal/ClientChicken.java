package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientChicken extends ClientEntity implements ReflectableEntity {

    private ExpoAnimationHandler animationHandler;
    private boolean cachedMoving;
    private boolean flipped;

    private int variant;

    private float damageDelta;
    private boolean damageTint;

    private int lastFootstepIndex;

    @Override
    public void onCreation() {
        animationHandler = new ExpoAnimationHandler() {
            @Override
            public void onAnimationFinish() {
                if(isInWater() && animationHandler.getActiveAnimationName().equals("idle")) spawnPuddle(false, 0, 1);
                lastFootstepIndex = 0;
            }
        };
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_chicken_var_" + variant + "_idle", 2, 0.75f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_chicken_var_" + variant + "_walk", 8, 0.1f));

        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("bloody_squish");
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        damageDelta = RenderContext.get().deltaTotal;
        damageTint = true;
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
        animationHandler.tick(delta);
        boolean flip = (!flipped && serverDirX == -1) || (flipped && serverDirX == 1);

        if(flip) {
            animationHandler.flipAllAnimations(true, false);
            flipped = !flipped;
        }

        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f);

        int i = animationHandler.getActiveAnimation().getFrameIndex();

        if((i == 3 || i == 7) && (lastFootstepIndex != i)) {
            lastFootstepIndex = i;

            playEntitySound(getFootstepSound(), 0.4f);

            if(isInWater()) spawnPuddle(false, 0, 1);
        }

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
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public boolean isMoving() {
        return serverMoveDistance > 0;
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(animationHandler.getActiveFrame());
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CHICKEN;
    }

}
