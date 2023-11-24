package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.util.EntityMetadataMapper;

public class ClientHealthBar extends ClientEntity implements TopVisibilityEntity {

    public ClientEntity parentEntity = null;

    @Override
    public void onCreation() {

    }

    @Override
    public void onDeletion() {
        parentEntity = null;
    }

    @Override
    public void tick(float delta) {
        updateDepth();
    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.HEALTH_BAR;
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        drawHealthBar(rc);
    }

    public void drawHealthBar(RenderContext rc) {
        var meta = EntityMetadataMapper.get().getFor(parentEntity.getEntityType().ENTITY_SERVER_TYPE);
        String name = parentEntity.getEntityType() == ClientEntityType.PLAYER ? null : meta.getName();
        float offsetY = parentEntity.getEntityType() == ClientEntityType.PLAYER ? 8 : 0;
        drawHealthBar(rc, parentEntity.serverHealth / meta.getMaxHealth(), parentEntity.textureWidth, offsetY, name);
    }

    public void drawHealthBar(RenderContext rc, float healthPercentage, float barWidth, float offsetY, String suppliedName) {
        float diff = rc.deltaTotal - parentEntity.lastBlink;
        float MAX_DIFF = 2.5f;
        if(diff >= MAX_DIFF || parentEntity.serverHealth <= 0) {
            entityManager().removeEntity(this);
            return;
        }
        float TRANS_DUR = 0.25f;

        float alpha = 1.0f;

        if(diff <= TRANS_DUR) {
            float dt = diff / TRANS_DUR;
            alpha = Interpolation.smooth2.apply(dt);
        } else if(diff >= (MAX_DIFF - TRANS_DUR)) {
            float dt = 1f - (diff - (MAX_DIFF - TRANS_DUR)) / TRANS_DUR;
            alpha = Interpolation.smooth2.apply(dt);
        }

        if(alpha <= 0) return;

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, alpha);

        RenderContext r = RenderContext.get();
        float length = Math.max(barWidth, 24) - 2;

        float startX = parentEntity.finalTextureCenterX - length * 0.5f - 1;
        float startY = parentEntity.clientPosY + parentEntity.textureHeight + 8 + offsetY;

        rc.arraySpriteBatch.draw(r.hbEdge, startX, startY);
        rc.arraySpriteBatch.draw(r.hbEdge, startX + length + 1, startY);

        float lengthFilled = (healthPercentage * length);
        float remaining = length - lengthFilled;

        rc.arraySpriteBatch.draw(r.hbFilled, startX + 1, startY, lengthFilled, 3);
        rc.arraySpriteBatch.draw(r.hbUnfilled, startX + 1 + lengthFilled, startY, remaining, 3);

        rc.arraySpriteBatch.setColor(Color.WHITE);

        // Draw entity name
        if(suppliedName != null) {
            rc.globalGlyph.setText(rc.m5x7_border_all[0], suppliedName);

            float _x = startX + 1 + (length - rc.globalGlyph.width) * 0.5f;
            float _y = startY + rc.globalGlyph.height + 8;

            rc.m5x7_border_all[0].setColor(1.0f, 1.0f, 1.0f, alpha);
            rc.m5x7_border_all[0].draw(rc.arraySpriteBatch, suppliedName, _x, _y);
            rc.m5x7_border_all[0].setColor(Color.WHITE);
        }
    }

}