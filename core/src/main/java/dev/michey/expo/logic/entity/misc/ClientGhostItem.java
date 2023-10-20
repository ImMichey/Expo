package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;

import static dev.michey.expo.render.RenderContext.TRANS_100_PACKED;

public class ClientGhostItem extends ClientEntity implements AmbientOcclusionEntity {

    public TextureRegion texture;
    public int amount;

    public float floatingPos;
    public float floatingPosAnimation;
    private float useAlpha = 1.0f;

    @Override
    public void onCreation() {
        visibleToRenderEngine = true;
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        useAlpha -= delta * 2;
        floatingPosAnimation += delta * 30;

        if(useAlpha <= 0) {
            entityManager().removeEntity(this);
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateDepth();
        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float SCALE_X = 0.75f;
        float SCALE_Y = 0.75f;

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, useAlpha);
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY + floatingPos + floatingPosAnimation, texture.getRegionWidth() * SCALE_X, texture.getRegionHeight() * SCALE_Y);

        String numberAsString = String.valueOf(amount);

        float slotW = ExpoClientContainer.get().getPlayerUI().invSlot.getRegionWidth();
        float vx = finalTextureStartX;
        float vy = finalTextureStartY;
        float fontScale = 0.5f;

        float ex = (slotW - texture.getRegionWidth()) * 0.5f * SCALE_X + texture.getRegionWidth() * SCALE_X + vx - (numberAsString.length() * 6 * fontScale) - 1 * SCALE_X;
        float y = vy + floatingPosAnimation + floatingPos - 7 * fontScale;

        int add = 0;

        for(char c : numberAsString.toCharArray()) {
            TextureRegion indiNumber = rc.getNumber(Integer.parseInt(String.valueOf(c)));
            rc.arraySpriteBatch.draw(indiNumber, ex + add, y, indiNumber.getRegionWidth() * fontScale, indiNumber.getRegionHeight() * fontScale);
            add += (int) (6 * fontScale);
        }

        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public void renderAO(RenderContext rc) {
        float norm = (floatingPos + 2) / 4f; // [0-4] // 0 = max, 4 = lowest
        float MIN_SCALE = 0.5f;
        float MAX_SCALE = 1f;
        float _norm = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1f - norm);
        _norm *= useAlpha;

        float tw = rc.aoTexture.getWidth();
        float th = rc.aoTexture.getHeight();
        float relative = (texture.getRegionWidth() * 0.75f + texture.getRegionHeight() * 0.75f) / 1.5f / tw * _norm;

        float packed = new Color(0.0f, 0.0f, 0.f, useAlpha).toFloatBits();
        if(rc.aoBatch.getPackedColor() != packed) rc.aoBatch.setPackedColor(packed);
        rc.aoBatch.draw(rc.aoTexture, clientPosX - tw * 0.5f * relative, clientPosY - 4 - th * 0.5f * relative, tw * relative, th * relative);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.GHOST_ITEM;
    }

}
