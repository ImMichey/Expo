package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.light.ExpoLight;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientFirefly extends ClientEntity {

    private final ExpoAnimationHandler animationHandler;
    private boolean flipped;
    private boolean cachedMoving;

    private float damageDelta;
    private boolean damageTint;

    private float fadeInDelta;

    private final float flightHeight = 16;
    private ExpoLight fireflyLight;

    public ClientFirefly() {
        animationHandler = new ExpoAnimationHandler();
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_firefly_fly", 4, 0.250f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_firefly_fly", 4, 0.125f));
        removalFade = 0.25f;
        fadeInDelta = 0.25f;
    }

    @Override
    public void onCreation() {
        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f.getRegionWidth(), f.getRegionHeight(), 0, 0, -f.getRegionWidth() * 0.5f, flightHeight);

        fireflyLight = new ExpoLight(80.0f, 40, 1f, 0.3f);
        fireflyLight.color(1.0f, 0.882f, 0.0f, 1.0f);
    }

    @Override
    public void onDeletion() {
        fireflyLight.delete();

        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("bloody_squish");
        }
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        damageDelta = RenderContext.get().deltaTotal;
        damageTint = true;
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        fireflyLight.update(finalDrawPosX, finalDrawPosY);

        if(fadeInDelta > 0) {
            fadeInDelta -= delta;
            if(fadeInDelta < 0) fadeInDelta = 0;

            fireflyLight.colorAlpha(1.0f - fadeInDelta * 4);
        }

        if(cachedMoving != isMoving()) {
            cachedMoving = !cachedMoving;
            animationHandler.reset();
            animationHandler.switchToAnimation(cachedMoving ? "walk" : "idle");
        }
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
        updateTextureBounds(f.getRegionWidth(), f.getRegionHeight(), 0, 0, -f.getRegionWidth() * 0.5f, flightHeight);

        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(-flightHeight);
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            if(damageTint) {
                float MAX_TINT_DURATION = 0.2f;
                if(RenderContext.get().deltaTotal - damageDelta >= MAX_TINT_DURATION) damageTint = false;
            }

            if(damageTint) {
                rc.arraySpriteBatch.setColor(ClientStatic.COLOR_DAMAGE_TINT);
            } else {
                if(fadeInDelta > 0) {
                    rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f - (fadeInDelta * 4));
                } else {
                    rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, removalFade * 4);
                }
            }
            rc.arraySpriteBatch.draw(animationHandler.getActiveFrame(), finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX, finalTextureStartY - flightHeight * 2, 0, flightHeight);
        float[] mushroomVertices = rc.arraySpriteBatch.obtainShadowVertices(animationHandler.getActiveFrame(), shadow);
        boolean drawMushroom = rc.verticesInBounds(mushroomVertices);

        if(drawMushroom) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(animationHandler.getActiveFrame(), textureWidth, textureHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FIREFLY;
    }

}
