package dev.michey.expo.logic.entity.hostile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientSlime extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    private final ExpoAnimationHandler animationHandler;
    private float simulatedHeight;
    private static final float MAX_HEIGHT = 8f;

    public ClientSlime() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_slime_green_idle", 4, 0.5f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_slime_green_jump", 20, 0.03f));
        animationHandler.addFootstepOn(new String[] {"walk"}, 14);
        animationHandler.setPuddleData(new ExpoAnimationHandler.PuddleData[] {
                new ExpoAnimationHandler.PuddleData("walk", 19, true, 0, 0),
        });
    }

    @Override
    public void playFootstepSound() {
        playEntitySound(getFootstepSound(), 0.6f);
    }

    @Override
    public String getFootstepSound() {
        if(isInWater()) {
            return "step_water";
        }

        return "slime";
    }

    @Override
    public void onCreation() {

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
        ParticleSheet.Common.spawnBloodParticlesSlime(this);
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
        updateTextureBounds(cf.getRegionWidth(), cf.getRegionHeight() + MAX_HEIGHT, 0, 0);

        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta, 7.5f);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);

            if(animationHandler.getActiveAnimationName().equals("walk")) {
                float norm = animationHandler.getActiveAnimation().getProgress();
                float simulationInterpolation;

                if(norm <= 0.5f) {
                    simulationInterpolation = norm * 2;
                } else {
                    simulationInterpolation = 1f - (norm - 0.5f) * 2;
                }

                simulatedHeight = Interpolation.exp10.apply(simulationInterpolation) * MAX_HEIGHT;
            }

            updateDepth();
            rc.useArrayBatch();
            chooseArrayBatch(rc, interpolatedBlink);

            blinkDelta += delta;
            float PLAYER_BLINK_COOLDOWN = 3.0f;
            float PLAYER_BLINK_DURATION = 0.25f;
            if(blinkDelta >= PLAYER_BLINK_COOLDOWN) blinkDelta = -PLAYER_BLINK_DURATION;

            rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY + simulatedHeight);

            rc.useRegularArrayShader();
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion cf = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(cf, finalDrawPosX, finalDrawPosY - simulatedHeight, cf.getRegionWidth(), cf.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        TextureRegion activeFrame = animationHandler.getActiveFrame();

        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX, finalTextureStartY, 0, simulatedHeight);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(activeFrame, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradientSemiTransparent(activeFrame, textureWidth, textureHeight - MAX_HEIGHT, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SLIME;
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO50(rc, 0.4f - 0.2f * simulatedHeight / MAX_HEIGHT, 0.5f - 0.25f * simulatedHeight / MAX_HEIGHT, 0, 0.5f);
    }

}
