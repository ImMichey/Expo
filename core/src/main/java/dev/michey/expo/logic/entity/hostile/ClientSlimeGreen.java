package dev.michey.expo.logic.entity.hostile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientSlimeGreen extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private final ExpoAnimationHandler animationHandler;

    public ClientSlimeGreen() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_slime_green_walk", 4, 0.5f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_slime_green_walk", 4, 0.15f));
        animationHandler.addFootstepOn(new String[] {"walk"}, 1);
    }

    @Override
    public void playFootstepSound() {
        playEntitySound(getFootstepSound());
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
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        setBlink();
        ParticleSheet.Common.spawnBloodParticles(this, 0, 0);
        spawnHealthBar(damage);
        spawnDamageIndicator((int) damage, clientPosX, clientPosY + textureHeight + 28, entityManager().getEntityById(damageSourceEntityId));
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(isMoving()) {
            calculateReflection();
        }
    }

    @Override
    public boolean isMoving() {
        return serverMoveDistance > 0;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        TextureRegion cf = animationHandler.getActiveFrame();
        updateTextureBounds(cf);

        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta, 7.5f);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);

            updateDepth();
            rc.useArrayBatch();
            chooseArrayBatch(rc, interpolatedBlink);

            blinkDelta += delta;
            float PLAYER_BLINK_COOLDOWN = 3.0f;
            float PLAYER_BLINK_DURATION = 0.25f;
            if(blinkDelta >= PLAYER_BLINK_COOLDOWN) blinkDelta = -PLAYER_BLINK_DURATION;

            rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY);

            rc.useRegularArrayShader();
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion cf = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY, cf.getRegionWidth(), cf.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(animationHandler.getActiveFrame());
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SLIME_GREEN;
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.25f, 0.25f, 0, 0);
    }

}
