package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;
import dev.michey.expo.util.EntityRemovalReason;

import static dev.michey.expo.render.RenderContext.TRANS_100_PACKED;

public class ClientItem extends ClientEntity implements ReflectableEntity, AmbientOcclusionEntity {

    public static final float DEFAULT_SCALE = 0.875f;
    public static final float SCALE_DURATION = 0.25f;

    public int itemId;
    public int itemAmount;

    public ItemRender[] ir;
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

    private float bounceDelta;
    private float bounce;

    @Override
    public void onCreation() {
        ir = ItemMapper.get().getMapping(itemId).uiRender;
        currentScaleX = DEFAULT_SCALE;
        currentScaleY = DEFAULT_SCALE;

        updateTextureBounds(ir[0].useWidth * currentScaleX, ir[0].useHeight * currentScaleY, 0, 0);
        playEntitySound("pop", 0.5f);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            spawnGhostEntity(itemAmount);
        }
    }
    @Override
    public void tick(float delta) {
        if(syncPositionWithServer()) {
            updateTexturePositionData();
        }

        if(isMoving()) {
            calculateReflection();
        }

        lifetime += delta;

        /*
        if(lifetime <= SCALE_DURATION) {
            float norm = Math.abs(lifetime / SCALE_DURATION - 1);
            currentScaleX = DEFAULT_SCALE + 0.625f * norm;
            currentScaleY = DEFAULT_SCALE + 0.625f * norm;
        } else {
            currentScaleX = DEFAULT_SCALE;
            currentScaleY = DEFAULT_SCALE;
        }
        */

        if(lifetime <= 0.05f) {
            useAlpha = lifetime / 0.05f;
        } else {
            useAlpha = 1.0f;
        }

        float PHASE_1_SPEED = 5.0f;
        float PHASE_2_SPEED = 1.5f;
        float HEIGHT = 16.0f;

        if(bounceDelta < 1.0f) {
            bounceDelta += delta * PHASE_1_SPEED;
            if(bounceDelta > 1.0f) {
                bounceDelta = 1.0f;
            }
            bounce = Interpolation.smooth.apply(bounceDelta) * HEIGHT;
        } else if(bounceDelta < (2.0f)) {
            bounceDelta += delta * PHASE_2_SPEED;
            if(bounceDelta > 2.0f) {
                bounceDelta = 2.0f;
            }
            bounce = HEIGHT - Interpolation.bounceOut.apply(bounceDelta - 1.0f) * HEIGHT;
        } else {
            bounce = 0;
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

            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, useAlpha);

            for(ItemRender ir : ir) {
                TextureRegion tr = ir.useTextureRegion;

                rc.arraySpriteBatch.draw(tr,
                        finalDrawPosX + (ir.offsetX) * currentScaleX - (tr.getRegionWidth() * (stackX - 1.0f) * 0.5f * currentScaleX),
                        finalDrawPosY + floatingPos + ir.offsetY * currentScaleY + bounce,
                        tr.getRegionWidth() * stackX * currentScaleX,
                        tr.getRegionHeight() * stackY * currentScaleY);
            }

            if(itemAmount > 1 || !ItemMapper.get().getMapping(itemId).logic.isSpecialType()) {
                String numberAsString = String.valueOf(itemAmount);
                BitmapFont uf = rc.m6x11_border_all[0];
                rc.globalGlyph.setText(uf, numberAsString);
                float fontScale = (currentScaleX + 0.125f) * 0.5f;
                float fw = rc.globalGlyph.width * fontScale;

                uf.getData().setScale(fontScale);
                uf.setColor(1.0f, 1.0f, 1.0f, useAlpha);

                float slotW = ExpoClientContainer.get().getPlayerUI().invSlot.getRegionWidth();
                float vx = finalTextureStartX;
                float vy = finalTextureStartY;
                float ex = (slotW - ir[0].useWidth) * 0.5f * currentScaleX + ir[0].useWidth * currentScaleX + vx - fw - currentScaleX;
                float y = vy + floatingPos + bounce;

                uf.draw(rc.arraySpriteBatch, numberAsString, ex, y);

                uf.setColor(Color.WHITE);
                uf.getData().setScale(1.0f);
            }

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        float dsp = textureWidth - textureWidth * stackX;

        for(ItemRender ir : ir) {
            TextureRegion tr = ir.useTextureRegion;

            rc.arraySpriteBatch.draw(tr,
                    finalDrawPosX + dsp * 0.5f + ir.offsetX * currentScaleX,
                    finalDrawPosY - floatingPos - 3 - ir.offsetY * currentScaleY - bounce,
                    tr.getRegionWidth() * stackX * currentScaleX,
                    tr.getRegionHeight() * stackY * currentScaleY * -1);
        }
    }

    private void spawnGhostEntity(int amount) {
        ClientGhostItem ghostItem = new ClientGhostItem();
        ghostItem.ir = ir;
        ghostItem.amount = amount;
        ghostItem.itemId = itemId;
        ghostItem.clientPosX = clientPosX;
        ghostItem.clientPosY = clientPosY;
        ghostItem.floatingPos = floatingPos + bounce;
        ghostItem.scaleX = currentScaleX;
        ghostItem.scaleY = currentScaleY;
        ghostItem.stealTextureData(this);
        entityManager().addClientSideEntity(ghostItem);
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
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

        float tw = rc.aoTexture.getWidth();
        float th = rc.aoTexture.getHeight();
        float relative = (textureWidth + textureHeight) / 1.5f / tw * _norm;

        if(rc.aoBatch.getPackedColor() != TRANS_100_PACKED) rc.aoBatch.setPackedColor(TRANS_100_PACKED);
        rc.aoBatch.draw(rc.aoTexture, clientPosX - tw * 0.5f * relative, clientPosY - 4 - th * 0.5f * relative, tw * relative, th * relative);
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        itemId = (int) payload[0];
        itemAmount = (int) payload[1];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ITEM;
    }

}
