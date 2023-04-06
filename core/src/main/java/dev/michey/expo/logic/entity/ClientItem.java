package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientItem extends ClientEntity {

    public int itemId;
    public int itemAmount;

    public TextureRegion texture;
    public float currentScaleX, currentScaleY;
    public float floatingPos;
    private final float floatingOffset = MathUtils.random(360f);
    private final float shimmerOffset = MathUtils.random(360f);

    private float lifetime;
    private float useAlpha;

    @Override
    public void onCreation() {
        texture = ItemMapper.get().getMapping(itemId).uiRender.textureRegion;
        currentScaleX = 0.75f;
        currentScaleY = 0.75f;
        updateTexture(-texture.getRegionWidth() * 0.5f * currentScaleX, 0, texture.getRegionWidth() * currentScaleX, texture.getRegionHeight() * currentScaleY);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            spawnGhostEntity(itemAmount);
        }
    }

    @Override
    public void onDamage(float damage, float newHealth) {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        lifetime += delta;

        if(lifetime <= 0.25f) {
            useAlpha = lifetime / 0.25f;
        } else {
            useAlpha = 1.0f;
        }
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

            float textureX = clientPosX - texture.getRegionWidth() * 0.5f * currentScaleX;
            float textureY = clientPosY + floatingPos;
            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, useAlpha);
            rc.arraySpriteBatch.draw(texture, textureX, textureY, drawWidth, drawHeight);

            String numberAsString = itemAmount + "";

            float slotW = ExpoClientContainer.get().getPlayerUI().invSlot.getRegionWidth();
            float vx = clientPosX + drawOffsetX;
            float vy = clientPosY + drawOffsetY;
            float fontScale = 0.5f;

            float ex = (slotW - texture.getRegionWidth()) * 0.5f * currentScaleX + texture.getRegionWidth() * currentScaleX + vx - (numberAsString.length() * 6 * fontScale) - 1 * currentScaleX;
            float y = vy + floatingPos - 7 * fontScale;

            int add = 0;

            for(char c : numberAsString.toCharArray()) {
                TextureRegion indiNumber = rc.getNumber(Integer.parseInt(c + ""));
                rc.arraySpriteBatch.draw(indiNumber, ex + add, y, indiNumber.getRegionWidth() * fontScale, indiNumber.getRegionHeight() * fontScale);
                add += 6 * fontScale;
            }

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    private void spawnGhostEntity(int amount) {
        ClientGhostItem ghostItem = new ClientGhostItem();
        ghostItem.texture = texture;
        ghostItem.amount = amount;
        ghostItem.clientPosX = clientPosX;
        ghostItem.clientPosY = clientPosY;
        ghostItem.floatingPos = floatingPos;
        ghostItem.drawOffsetX = drawOffsetX;
        ghostItem.drawOffsetY = drawOffsetY;
        entityManager().addClientSideEntity(ghostItem);
    }

    @Override
    public void readEntityDataUpdate(Object[] payload) {
        int newAmount = (int) payload[0];
        spawnGhostEntity(itemAmount - newAmount);
        itemAmount = newAmount;
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX + drawOffsetX, clientPosY, 0, floatingPos);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.arraySpriteBatch.drawGradient(texture, drawWidth, drawHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ITEM;
    }

}
