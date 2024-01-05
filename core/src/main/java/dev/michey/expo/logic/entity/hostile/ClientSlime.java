package dev.michey.expo.logic.entity.hostile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientSlime extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    //private final ExpoAnimationHandler animationHandler;
    private TextureRegion slime;

    public ClientSlime() {
        /*
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("woodfolk_idle_bob", 2, 0.5f, 1.0f));
        animationHandler.addAnimation("idle", new ExpoAnimation("woodfolk_idle_bob_blink", 2, 0.5f, 0.25f));
        animationHandler.addAnimation("idle", new ExpoAnimation("woodfolk_idle_spin", 4, 0.5f, 0.75f));
        animationHandler.addAnimation("idle", new ExpoAnimation("woodfolk_idle_spin_blink", 4, 0.5f, 0.175f));

        animationHandler.addAnimation("walk", new ExpoAnimation("woodfolk_walk", 5, 0.075f));
        animationHandler.addFootstepOn(new String[] {"walk"}, 4);
        */
    }

    @Override
    public void playFootstepSound() {
        playEntitySound(getFootstepSound(), 0.4f);
    }

    @Override
    public void onCreation() {
        slime = tr("entity_slime_green");
        updateTextureBounds(slime);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("log_split");
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        setBlink();
        ParticleSheet.Common.spawnBloodParticlesWoodfolk(this);
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
        TextureRegion cf = slime;
        updateTextureBounds(cf);

        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta, 7.5f);

        if(visibleToRenderEngine) {
            //animationHandler.tick(delta);

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
        TextureRegion cf = slime;
        rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY, cf.getRegionWidth(), cf.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(slime);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SLIME;
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.25f, 0.25f, 0, 0);
    }

}
