package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;

public class ClientGhostItem extends ClientEntity {

    public TextureRegion texture;
    public int amount;

    private final float SCALE_X = 0.75f, SCALE_Y = 0.75f;
    public float floatingPos;
    public float floatingPosAnimation;
    private float useAlpha = 1.0f;

    @Override
    public void onCreation() {

    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        useAlpha -= delta * 2;
        floatingPosAnimation = Interpolation.pow3Out.apply(1.0f - useAlpha) * 10;

        if(useAlpha <= 0) {
            entityManager().removeEntity(this);
        }
    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void render(RenderContext rc, float delta) {
        updateDepth();
        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float textureX = clientPosX - texture.getRegionWidth() * 0.5f * SCALE_X;
        float textureY = clientPosY + floatingPos + floatingPosAnimation;
        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, useAlpha);
        rc.arraySpriteBatch.draw(texture, textureX, textureY, texture.getRegionWidth() * SCALE_X, texture.getRegionHeight() * SCALE_Y);

        String numberAsString = amount + "";

        float slotW = ExpoClientContainer.get().getPlayerUI().invSlot.getRegionWidth();
        float vx = clientPosX + drawOffsetX;
        float vy = clientPosY + drawOffsetY;
        float fontScale = 0.5f;

        float ex = (slotW - texture.getRegionWidth()) * 0.5f * SCALE_X + texture.getRegionWidth() * SCALE_X + vx - (numberAsString.length() * 6 * fontScale) - 1 * SCALE_X;
        float y = vy + floatingPosAnimation + floatingPos - 7 * fontScale;

        int add = 0;

        for(char c : numberAsString.toCharArray()) {
            TextureRegion indiNumber = rc.getNumber(Integer.parseInt(c + ""));
            rc.arraySpriteBatch.draw(indiNumber, ex + add, y, indiNumber.getRegionWidth() * fontScale, indiNumber.getRegionHeight() * fontScale);
            add += 6 * fontScale;
        }

        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.GHOST_ITEM;
    }

}
