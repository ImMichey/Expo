package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;

public class ClientRaindrop extends ClientEntity {

    private float scale;
    private float rotation;
    private float groundY;
    private float velocityX, velocityY;
    private boolean splash;
    private float animationDelta;

    private Sprite raindropSprite;
    private Animation<TextureRegion> splashAnimation;
    private float animationAlpha = 1.0f;

    @Override
    public void onCreation() {
        raindropSprite = new Sprite(ExpoAssets.get().textureRegion("raindrop"));
        raindropSprite.setPosition(clientPosX, clientPosY);
        raindropSprite.setScale(scale);
        raindropSprite.setRotation(rotation);

        Array<TextureRegion> tex = new Array<>(4);
        tex.add(ExpoAssets.get().textureRegion("rainsplash_1"));
        tex.add(ExpoAssets.get().textureRegion("rainsplash_2"));
        tex.add(ExpoAssets.get().textureRegion("rainsplash_3"));
        tex.add(ExpoAssets.get().textureRegion("rainsplash_4"));

        splashAnimation = new Animation<>(0.15f, tex);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        if(!splash) {
            raindropSprite.translate(velocityX * delta, velocityY * delta);
            clientPosX += velocityX * delta;
            clientPosY += velocityY * delta;

            if(clientPosY <= groundY) {
                splash = true;
                clientPosX -= 3;
                updateTexture(0, 0, 3.5f, 2.5f);
            }
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        drawnLastFrame = rc.inDrawBounds(this);

        if(splash) {
            animationDelta += delta;

            if(splashAnimation.isAnimationFinished(animationDelta)) {
                entityManager().removeEntity(this);
            } else {
                if(drawnLastFrame) {
                    TextureRegion anim = splashAnimation.getKeyFrame(animationDelta, false);

                    rc.useBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_ARRAY_SHADER);

                    if(animationAlpha < 1.0f) {
                        rc.currentBatch.setColor(1.0f, 1.0f, 1.0f, animationAlpha);
                        rc.currentBatch.draw(anim, clientPosX, clientPosY, anim.getRegionWidth() * 0.5f, anim.getRegionHeight() * 0.5f);
                        rc.currentBatch.setColor(Color.WHITE);
                    } else {
                        rc.currentBatch.draw(anim, clientPosX, clientPosY, anim.getRegionWidth() * 0.5f, anim.getRegionHeight() * 0.5f);
                    }
                }

                // 0.6 = fin
                if(animationDelta >= 0.3f) {
                    animationAlpha = 1f - ((animationDelta - 0.3f) / 0.3f * 0.5f);
                }
            }
        } else {
            if(drawnLastFrame) {
                rc.useBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_ARRAY_SHADER);
                raindropSprite.draw(rc.currentBatch);
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
        scale = 0.25f + MathUtils.random(0.15f);
        this.groundY = groundY;
        this.rotation = rotation;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        updateTexture(0, 0, scale, 4f * scale);
    }

}
