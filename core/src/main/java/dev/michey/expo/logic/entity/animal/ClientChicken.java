package dev.michey.expo.logic.entity.animal;

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

public class ClientChicken extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private ExpoAnimationHandler animationHandler;
    private int variant;

    @Override
    public void onCreation() {
        animationHandler = new ExpoAnimationHandler(this) {
            @Override
            public void onAnimationFinish() {
                if(isInWater() && animationHandler.getActiveAnimationName().equals("idle")) spawnPuddle(false, 0, 1);
            }
        };
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_chicken_var_" + variant + "_idle", 2, 0.75f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_chicken_var_" + variant + "_walk", 8, 0.1f));
        animationHandler.addFootstepOn(new String[] {"walk"}, 3, 7);

        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("bloody_squish");
        }
    }

    @Override
    public void playFootstepSound() {
        playEntitySound(getFootstepSound(), 0.4f);
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
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY, f.getRegionWidth(), f.getRegionHeight() * -1);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f);

        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta, 7.5f);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);

            updateDepth();
            rc.useArrayBatch();
            chooseArrayBatch(rc, interpolatedBlink);

            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY);

            rc.useRegularArrayShader();
        }
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAOAuto100(rc);
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
