package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.shadow.ShadowUtils;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientWorm extends ClientEntity {

    private final ExpoAnimationHandler animationHandler;

    private boolean cachedMoving;
    private boolean flipped;

    public ClientWorm() {
        animationHandler = new ExpoAnimationHandler();
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_wormS_idle", 3, 0.25f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_wormS_walk", 5, 0.15f));
    }

    @Override
    public void onCreation() {
        updateTextureBounds(animationHandler.getActiveFrame());
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(cachedMoving != isMoving()) {
            cachedMoving = !cachedMoving;
            animationHandler.reset();
            animationHandler.switchToAnimation(cachedMoving ? "walk" : "idle");
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            animationHandler.tick(delta);
            boolean flip = (!flipped && serverDirX == 0) || (flipped && serverDirX == 1);

            if(flip) {
                animationHandler.flipAllAnimations(true, false);
                flipped = !flipped;
            }

            TextureRegion f = animationHandler.getActiveFrame();
            updateTextureBounds(f);

            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
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
        return ClientEntityType.WORM;
    }

}
