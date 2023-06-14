package dev.michey.expo.logic.entity.animal;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.shadow.ShadowUtils;

public class ClientCrab extends ClientEntity {

    private final ExpoAnimationHandler animationHandler;
    private TextureRegion debugTex;

    private boolean cachedMoving;
    private boolean flipped;

    private float delta;

    public ClientCrab() {
        animationHandler = new ExpoAnimationHandler();
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_wormS_idle", 3, 0.25f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_wormS_walk", 5, 0.15f));
    }

    @Override
    public void onCreation() {
        debugTex = tr("entity_crab");
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        this.delta += delta;

        if(this.delta >= 6.0f) {
            this.delta = MathUtils.random(1.0f, 3.0f);
            playEntitySound("crab");
        }

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

            updateTextureBounds(debugTex);

            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(debugTex, clientPosX, clientPosY, textureWidth, textureHeight);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        float[] mushroomVertices = rc.arraySpriteBatch.obtainShadowVertices(debugTex, shadow);
        boolean drawMushroom = rc.verticesInBounds(mushroomVertices);

        if(drawMushroom) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(debugTex, textureWidth, textureHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CRAB;
    }

}
