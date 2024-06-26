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

public class ClientMaggot extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private final ExpoAnimationHandler animationHandler;

    public ClientMaggot() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_maggot_idle", 3, 0.25f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_maggot_walk", 5, 0.15f));
        animationHandler.getActiveAnimation().randomOffset();
    }

    @Override
    public void onCreation() {
        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void onDeletion() {
        if(removalReason.isKillReason()) {
            playEntitySound("bloody_squish");
            ParticleSheet.Common.spawnGoreParticles(animationHandler.getActiveFrame(), clientPosX, clientPosY);
        }
    }

    @Override
    public boolean isMoving() {
        return serverMoveDistance > 0;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        setBlink();
        spawnHealthBar(damage);
        spawnDamageIndicator(damage, clientPosX, clientPosY + textureHeight + 28, entityManager().getEntityById(damageSourceEntityId));
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
        }
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.25f, 0.25f, flipped ? 2 : -2, 0.5f);
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
