package dev.michey.expo.logic.entity.hostile;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.animator.ExpoAnimationHandler;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientZombie extends ClientEntity implements ReflectableEntity {

    private final ExpoAnimationHandler animationHandler;
    private final TextureRegion blink;
    private boolean flipped;
    private boolean cachedMoving;

    private float damageDelta;
    private boolean damageTint;

    private float blinkDelta;
    private static final float[] blinkOffset = {20, 19, 20, 20, 21, 20, 19, 20, 20, 21};
    private int lastFootstepIndex;

    public ClientZombie() {
        animationHandler = new ExpoAnimationHandler();
        animationHandler.addAnimation("idle", new ExpoAnimation("entity_zombie_idle", 2, 0.5f));
        animationHandler.addAnimation("walk", new ExpoAnimation("entity_zombie_walk", 10, 0.15f));

        blink = trn("entity_zombie_blink");
    }

    @Override
    public void onCreation() {
        TextureRegion f = animationHandler.getActiveFrame();
        updateTextureBounds(f.getRegionWidth(), f.getRegionHeight(), 0, 0, !flipped ? -4f : -12f, 0f);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("bloody_squish");
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        damageDelta = RenderContext.get().deltaTotal;
        damageTint = true;
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(isMoving()) {
            calculateReflection();
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

        int i = animationHandler.getActiveAnimation().getFrameIndex();
        TextureRegion f = animationHandler.getActiveAnimation().getFrame(i);
        updateTextureBounds(f.getRegionWidth(), f.getRegionHeight(), 0, 0, !flipped ? -5f : -13f, 0f);

        if((i == 2 || i == 7) && (lastFootstepIndex != i)) {
            lastFootstepIndex = i;
            playEntitySound(getFootstepSound());

            if(isInWater()) spawnPuddle(false);
        }

        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            blinkDelta += delta;
            float PLAYER_BLINK_COOLDOWN = 3.0f;
            float PLAYER_BLINK_DURATION = 0.25f;
            if(blinkDelta >= PLAYER_BLINK_COOLDOWN) blinkDelta = -PLAYER_BLINK_DURATION;

            if(damageTint) {
                float MAX_TINT_DURATION = 0.2f;
                if(RenderContext.get().deltaTotal - damageDelta >= MAX_TINT_DURATION) damageTint = false;
            }

            if(damageTint) {
                rc.arraySpriteBatch.setColor(ClientStatic.COLOR_DAMAGE_TINT);
            } else {
                rc.arraySpriteBatch.setColor(Color.WHITE);
            }

            rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY);

            if(blinkDelta < 0) {
                boolean idle = animationHandler.getActiveAnimationName().equals("idle");

                float idleBlinkX = flipped ? 9 : 5;
                float idleBlinkY = 20;

                if(idle) {
                    if(i == 1) idleBlinkY -= 1;
                } else {
                    idleBlinkY = blinkOffset[i];
                }

                rc.arraySpriteBatch.draw(blink, finalDrawPosX + idleBlinkX, finalDrawPosY + idleBlinkY);
            }

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        TextureRegion f = animationHandler.getActiveFrame();
        rc.arraySpriteBatch.draw(f, finalDrawPosX, finalDrawPosY, f.getRegionWidth(), f.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX, finalTextureStartY, 0, 0);
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
        return ClientEntityType.ZOMBIE;
    }

}
