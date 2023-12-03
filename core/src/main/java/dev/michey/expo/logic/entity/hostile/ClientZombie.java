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

public class ClientZombie extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private final ExpoAnimationHandler animationHandler;
    private final TextureRegion blink;

    private float blinkDelta;
    private static final float[] blinkOffset = {20, 19, 20, 20, 21, 20, 19, 20, 20, 21};

    public ClientZombie() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_zombie_idle", 2, 0.5f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_zombie_walk", 10, 0.15f));
        animationHandler.addFootstepOn(new String[] {"walk"}, 2, 7);

        blink = trn("entity_zombie_blink");
    }

    @Override
    public void playFootstepSound() {
        playEntitySound(getFootstepSound(), 0.5f);
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

            if(blinkDelta < 0) {
                boolean idle = animationHandler.getActiveAnimationName().equals("idle");

                float idleBlinkX = flipped ? 9 : 5;
                float idleBlinkY = 20;

                int index = animationHandler.getActiveAnimation().getFrameIndex();

                if(idle) {
                    if(index == 1) idleBlinkY -= 1;
                } else {
                    idleBlinkY = blinkOffset[index];
                }

                rc.arraySpriteBatch.draw(blink, finalDrawPosX + idleBlinkX, finalDrawPosY + idleBlinkY);
            }

            rc.useRegularArrayShader();
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY, f.getRegionWidth(), f.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(animationHandler.getActiveFrame());
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ZOMBIE;
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.25f, 0.25f, 0, 0);
    }

}
