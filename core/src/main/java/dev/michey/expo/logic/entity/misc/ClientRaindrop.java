package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;

public class ClientRaindrop extends ClientEntity {

    private float scale;
    private float rotation;
    private float groundY;
    private float velocityX, velocityY;
    private boolean splash;

    private Sprite raindropSprite;
    private float animationAlpha = 1.0f;

    private ExpoAnimation splashAnimation;

    @Override
    public void onCreation() {
        raindropSprite = new Sprite(ExpoAssets.get().textureRegion("raindrop"));
        raindropSprite.setPosition(clientPosX, clientPosY);
        raindropSprite.setScale(scale);
        raindropSprite.setRotation(rotation);

        splashAnimation = new ExpoAnimation("rainsplash", 4, 0.15f);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        if(!splash) {
            float x = velocityX * delta;
            float y = velocityY * delta;

            raindropSprite.translate(x, y);
            clientPosX += x;
            clientPosY += y;

            if(clientPosY <= groundY) {
                splash = true;
                clientPosX -= 3;
                updateTexture(0, 0, 3.5f, 2.5f);
            }
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(splash) {
            splashAnimation.tick(delta);

            if(splashAnimation.isAnimationFinished()) {
                entityManager().removeEntity(this);
            } else {
                if(visibleToRenderEngine) {
                    TextureRegion anim = splashAnimation.getFrame();

                    rc.useArrayBatch();

                    if(animationAlpha < 1.0f) {
                        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, animationAlpha);
                        rc.arraySpriteBatch.draw(anim, clientPosX, clientPosY, anim.getRegionWidth() * 0.5f, anim.getRegionHeight() * 0.5f);
                        rc.arraySpriteBatch.setColor(Color.WHITE);
                    } else {
                        rc.arraySpriteBatch.draw(anim, clientPosX, clientPosY, anim.getRegionWidth() * 0.5f, anim.getRegionHeight() * 0.5f);
                    }
                }

                // 0.6 = fin
                if(splashAnimation.getDelta() >= 0.3f) {
                    animationAlpha = 1f - ((splashAnimation.getDelta() - 0.3f) / 0.3f * 0.5f);
                }
            }
        } else {
            if(visibleToRenderEngine) {
                rc.useArrayBatch();
                raindropSprite.draw(rc.arraySpriteBatch);
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
        scale = 0.4f + MathUtils.random(0.1f);
        this.groundY = groundY;
        this.rotation = rotation;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        updateTexture(0, 0, scale, 4f * scale);
    }

}
