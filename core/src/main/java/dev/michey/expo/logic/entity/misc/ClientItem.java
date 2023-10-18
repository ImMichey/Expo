package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.util.EntityRemovalReason;

import static dev.michey.expo.render.RenderContext.TRANS_100_PACKED;

public class ClientItem extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    public int itemId;
    public int itemAmount;

    public TextureRegion texture;
    public float currentScaleX, currentScaleY;
    public float floatingPos;
    private final float floatingOffset = MathUtils.random(360f);
    private final float shimmerOffset = MathUtils.random(360f);

    private float lifetime;
    private float useAlpha;

    private boolean stackAnimation;
    private float stackAnimationDelta;
    private float stackX = 1.0f;
    private float stackY = 1.0f;

    private TextureRegion ao;

    @Override
    public void onCreation() {
        texture = ItemMapper.get().getMapping(itemId).uiRender.textureRegion;
        currentScaleX = 0.75f;
        currentScaleY = 0.75f;

        updateTextureBounds(texture.getRegionWidth() * currentScaleX, texture.getRegionHeight() * currentScaleY, 0, 0);
        ao = tr("item_shadow");
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            spawnGhostEntity(itemAmount);
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        updateTexturePositionData();
        lifetime += delta;

        if(lifetime <= 0.125f) {
            useAlpha = lifetime / 0.125f;
        } else {
            useAlpha = 1.0f;
        }

        if(stackAnimation) {
            stackAnimationDelta += delta;

            float c = MathUtils.cos(2 * (float) Math.PI * stackAnimationDelta) * 0.5f;
            if(c < 0) c = 0;

            stackX = 1f + c;
            stackY = 1f - c;

            float STACK_ANIMATION_DURATION = 0.25f;
            if(stackAnimationDelta >= STACK_ANIMATION_DURATION) {
                stackAnimation = false;
                stackAnimationDelta = 0f;
                stackX = 1f;
                stackY = 1f;
            }
        }
    }

    public void playStackAnimation() {
        stackAnimation = true;
        stackAnimationDelta = 0;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            float FLOATING_SPEED = 3.0f;
            float FLOATING_RANGE = 2.0f;
            float SHIMMER_SPEED = 24.0f;

            floatingPos = MathUtils.sin(rc.deltaTotal * FLOATING_SPEED + floatingOffset * FLOATING_SPEED) * FLOATING_RANGE;
            updateDepth();
            rc.useArrayBatch();
            if(rc.arraySpriteBatch.getShader() != rc.itemShineShader) rc.arraySpriteBatch.setShader(rc.itemShineShader);

            rc.itemShineShader.bind();
            rc.itemShineShader.setUniformf("u_delta", rc.deltaTotal * SHIMMER_SPEED + shimmerOffset * SHIMMER_SPEED);

            float dsp = textureWidth - textureWidth * stackX;

            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, useAlpha);
            rc.arraySpriteBatch.draw(texture, finalDrawPosX + dsp * 0.5f, finalDrawPosY + floatingPos, textureWidth * stackX, textureHeight * stackY);

            String numberAsString = String.valueOf(itemAmount);

            float slotW = ExpoClientContainer.get().getPlayerUI().invSlot.getRegionWidth();
            float vx = finalTextureStartX;
            float vy = finalTextureStartY;
            float fontScale = 0.5f;

            float ex = (slotW - texture.getRegionWidth()) * 0.5f * currentScaleX + texture.getRegionWidth() * currentScaleX + vx - (numberAsString.length() * 6 * fontScale) - 1 * currentScaleX;
            float y = vy + floatingPos - 7 * fontScale;

            int add = 0;

            for(char c : numberAsString.toCharArray()) {
                TextureRegion indiNumber = rc.getNumber(Integer.parseInt(String.valueOf(c)));
                rc.arraySpriteBatch.draw(indiNumber, ex + add - dsp * 0.5f, y, indiNumber.getRegionWidth() * fontScale, indiNumber.getRegionHeight() * fontScale);
                add += 6 * fontScale;
            }

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        float dsp = textureWidth - textureWidth * stackX;
        rc.arraySpriteBatch.draw(texture, finalDrawPosX + dsp * 0.5f, finalDrawPosY + floatingPos, textureWidth * stackX, textureHeight * stackY * -1);
    }

    private void spawnGhostEntity(int amount) {
        ClientGhostItem ghostItem = new ClientGhostItem();
        ghostItem.texture = texture;
        ghostItem.amount = amount;
        ghostItem.clientPosX = clientPosX;
        ghostItem.clientPosY = clientPosY;
        ghostItem.floatingPos = floatingPos;
        ghostItem.stealTextureData(this);
        entityManager().addClientSideEntity(ghostItem);
    }

    @Override
    public void readEntityDataUpdate(Object[] payload) {
        int newAmount = (int) payload[0];
        boolean ghostItem = (boolean) payload[1];
        if(ghostItem) spawnGhostEntity(itemAmount - newAmount);
        if(newAmount > itemAmount) playStackAnimation();
        itemAmount = newAmount;
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

        float tw = rc.aoTextures[0].getWidth();
        float th = rc.aoTextures[0].getHeight();
        float relative = (textureWidth + textureHeight) / 1.5f / tw * _norm;

        if(rc.aoBatch.getPackedColor() != TRANS_100_PACKED) rc.aoBatch.setPackedColor(TRANS_100_PACKED);
        rc.aoBatch.draw(rc.aoTextures[0], clientPosX - tw * 0.5f * relative, clientPosY - 4 - th * 0.5f * relative, tw * relative, th * relative);
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        itemId = (int) payload[0];
        itemAmount = (int) payload[1];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ITEM;
    }

}
