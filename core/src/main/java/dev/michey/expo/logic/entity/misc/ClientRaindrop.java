package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;

public class ClientRaindrop extends ClientEntity {

    private float rotation;
    private float groundY;
    private float velocityX, velocityY;
    private boolean splash;
    private float scale;

    private TextureRegion raindropTexture;
    private float animationAlpha = 1.0f;

    private ExpoAnimation splashAnimation;

    @Override
    public void onCreation() {
        raindropTexture = tr("raindrop");

        splashAnimation = new ExpoAnimation("rainsplash", 5, 0.15f);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        if(!splash) {
            float x = velocityX * delta;
            float y = velocityY * delta;
            clientPosX += x;
            clientPosY += y;

            if(clientPosY <= groundY) {
                splash = true;
                updateTextureBounds(9 * scale, 5 * scale, 0, 0);
            } else {
                updateTexturePositionData();
            }
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(splash) {
            splashAnimation.tick(delta);

            if(splashAnimation.isAnimationFinished() || !visibleToRenderEngine) {
                entityManager().removeEntity(this);
            } else {
                TextureRegion anim = splashAnimation.getFrame();
                float w = anim.getRegionWidth() * scale;
                rc.useArrayBatch();

                if(animationAlpha < 1.0f) {
                    rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, animationAlpha);
                    rc.arraySpriteBatch.draw(anim, clientPosX - w * 0.5f, clientPosY, w, anim.getRegionHeight() * scale);
                    rc.arraySpriteBatch.setColor(Color.WHITE);
                } else {
                    rc.arraySpriteBatch.draw(anim, clientPosX - w * 0.5f, clientPosY, w, anim.getRegionHeight() * scale);
                }

                // 0.6 = fin
                if(splashAnimation.getDelta() >= 0.375f) {
                    animationAlpha = 1f - ((splashAnimation.getDelta() - 0.375f) / 0.375f);
                }
            }
        } else {
            if(visibleToRenderEngine) {
                rc.useArrayBatch();
                rc.arraySpriteBatch.draw(raindropTexture, clientPosX, clientPosY, 0, 0,
                        raindropTexture.getRegionWidth() * scale, raindropTexture.getRegionHeight() * scale, 1.0f, 1.0f, rotation);
            }
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.RAINDROP;
    }

    public void initRaindrop(float x, float y, float groundY, float rotation, float velocityX, float velocityY) {
        clientPosX = x;
        clientPosY = y;
        this.scale = 0.4f + MathUtils.random(0.1f);
        this.groundY = groundY;
        this.rotation = rotation;
        this.velocityX = velocityX;
        this.velocityY = velocityY;

        updateTextureBounds(10 * scale, 10 * scale, 0, 0, -5 * scale, -5 * scale);
    }

}
