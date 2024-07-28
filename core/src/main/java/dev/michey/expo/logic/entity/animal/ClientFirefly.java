package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.light.ExpoLight;
import dev.michey.expo.render.shadow.ShadowUtils;

public class ClientFirefly extends ClientEntity {

    private final ExpoAnimationHandler animationHandler;

    private float fadeInDelta;

    private final float flightHeight = 16;
    private ExpoLight fireflyLight;

    public ClientFirefly() {
        animationHandler = new ExpoAnimationHandler(this);
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_firefly_fly", 4, 0.250f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_firefly_fly", 4, 0.125f));
        animationHandler.getActiveAnimation().randomOffset();
        removalFade = 0.25f;
        fadeInDelta = 0.25f;
    }

    @Override
    public boolean isMoving() {
        return serverMoveDistance > 0;
    }

    @Override
    public void onCreation() {
        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f.getRegionWidth(),
                f.getRegionHeight(), 0, 0, -f.getRegionWidth() * 0.5f, flightHeight);

        fireflyLight = new ExpoLight(80.0f, 32, 1f, 0.3f, false);
        fireflyLight.color(1.0f, 0.882f, 0.0f, 1.0f);
    }

    @Override
    public void onDeletion() {
        fireflyLight.delete();

        if(removalReason.isKillReason()) {
            playEntitySound("bloody_squish");
            ParticleSheet.Common.spawnGoreParticles(animationHandler.getActiveFrame(), clientPosX, finalDrawPosY);
        }
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
        fireflyLight.update(finalDrawPosX, finalDrawPosY, delta);

        if(fadeInDelta > 0) {
            fadeInDelta -= delta;
            if(fadeInDelta < 0) fadeInDelta = 0;

            fireflyLight.colorAlpha(1.0f - fadeInDelta * 4);
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f.getRegionWidth(), f.getRegionHeight(), 0, 0, -f.getRegionWidth() * 0.5f, flightHeight);

        visibleToRenderEngine = rc.inDrawBounds(this);
        float interpolatedBlink = tickBlink(delta);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);

            updateDepth(-flightHeight);
            rc.useArrayBatch();
            chooseArrayBatch(rc, interpolatedBlink);

            if(fadeInDelta > 0) {
                rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f - (fadeInDelta * 4));
            } else {
                rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, removalFade * 4);
            }
            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY, f.getRegionWidth(), f.getRegionHeight());
            rc.arraySpriteBatch.setColor(Color.WHITE);

            rc.useRegularArrayShader();
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX, finalTextureStartY - flightHeight * 2, 0, flightHeight);
        float[] mushroomVertices = rc.arraySpriteBatch.obtainShadowVertices(animationHandler.getActiveFrame(), shadow);
        boolean drawMushroom = rc.verticesInBounds(mushroomVertices);

        if(drawMushroom) {
            rc.arraySpriteBatch.drawGradient(animationHandler.getActiveFrame(), textureWidth, textureHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FIREFLY;
    }

}
