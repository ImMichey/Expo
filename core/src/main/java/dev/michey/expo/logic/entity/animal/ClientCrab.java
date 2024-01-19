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

public class ClientCrab extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private ExpoAnimationHandler animationHandler;
    private int variant;

    @Override
    public void onCreation() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_crab_variation_" + variant + "_idle", 4, 0.175f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_crab_variation_" + variant + "_walk", 12, 0.05f));
        animationHandler.addAnimation("attack", new ExpoAnimation("entity_crab_variation_" + variant + "_pinch", 8, 0.08f));
        animationHandler.addFootstepOn(new String[] {"walk"}, 4, 10);
        animationHandler.addAnimationSound("attack", "crab_snip", 5, 0.5f);
        animationHandler.setPuddleData(new ExpoAnimationHandler.PuddleData[] {
                new ExpoAnimationHandler.PuddleData("walk", 4, false, 0, 0),
                new ExpoAnimationHandler.PuddleData("walk", 10, false, 0, 0),
        });
        animationHandler.getActiveAnimation().randomOffset();

        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void playFootstepSound() {
        playEntitySound(getFootstepSound(), 0.3f);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        setBlink();
        ParticleSheet.Common.spawnBloodParticles(this, 0, -1.5f);

        spawnDamageIndicator(damage, clientPosX, clientPosY + textureHeight + 28, entityManager().getEntityById(damageSourceEntityId));
        spawnHealthBar(damage);
    }

    @Override
    public void playEntityAnimation(int animationId) {
        if(animationId == 0) {
            animationHandler.reset();
            animationHandler.switchToAnimation("attack");
        }
    }

    @Override
    public void onDeletion() {
        if(removalReason.isKillReason()) {
            playEntitySound("bloody_squish");
            ParticleSheet.Common.spawnGoreParticles(animationHandler.getActiveFrame(), clientPosX, clientPosY);
        }
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

            rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY);

            rc.useRegularArrayShader();
        }
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
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
