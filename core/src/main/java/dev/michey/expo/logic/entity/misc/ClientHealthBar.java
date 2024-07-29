package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.font.GradientFont;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.util.EntityMetadata;
import dev.michey.expo.server.util.EntityMetadataMapper;

public class ClientHealthBar extends ClientEntity implements TopVisibilityEntity {

    public ClientEntity parentEntity = null;
    public float damage;
    public float removalDelta;
    public float removalDelay;

    private Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

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
        var meta = EntityMetadataMapper.get().getFor(parentEntity.getEntityType().ENTITY_SERVER_TYPE);

        drawHealthBar(rc, meta, parentEntity.textureWidth);
    }

    public void drawHealthBar(RenderContext rc, EntityMetadata meta, float barWidth) {
        float diff = rc.deltaTotal - parentEntity.lastBlink;
        float MAX_DIFF = 2.5f;

        if(diff >= MAX_DIFF || parentEntity.serverHealth <= 0) {
            entityManager().removeEntity(this);
            return;
        }

        // ############################# ALPHA CHECK
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
        // ############################# ALPHA CHECK

        float healthPercentage = parentEntity.serverHealth / meta.getMaxHealth();
        String suppliedName = parentEntity.getEntityType() == ClientEntityType.PLAYER ? null : Lang.str("entity." + parentEntity.getEntityType().name().toLowerCase());
        float offsetY = meta.getHealthBarOffsetY();
        float length = Math.max(barWidth, 28) - 2;

        float animationThickness = 0;
        removalDelay -= rc.delta;

        if(removalDelta > 0) {
            float REMOVAL_SPEED = 3.0f;
            if(removalDelay <= 0) {
                removalDelta -= rc.delta * REMOVAL_SPEED;
            }

            if(removalDelta <= 0) {
                removalDelta = 0;
            } else {
                animationThickness = damage / meta.getMaxHealth() * length * Interpolation.pow2InInverse.apply(removalDelta);
            }
        }

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, alpha);

        RenderContext r = RenderContext.get();

        float startX = parentEntity.finalTextureCenterX - length * 0.5f - 1;
        float startY = parentEntity.clientPosY + parentEntity.textureHeight + 8 + offsetY;

        rc.arraySpriteBatch.draw(r.hbEdge, startX, startY);
        rc.arraySpriteBatch.draw(r.hbEdge, startX + length + 1, startY);

        float lengthFilled = (healthPercentage * length);
        float remaining = length - lengthFilled;

        rc.arraySpriteBatch.draw(r.hbFilled, startX + 1, startY, lengthFilled, 4);
        rc.arraySpriteBatch.draw(r.hbUnfilled, startX + 1 + lengthFilled, startY, remaining, 4);

        if(animationThickness > 0) {
            rc.arraySpriteBatch.draw(r.hbAnimation, startX + 1 + lengthFilled, startY, animationThickness, 4);
        }

        rc.arraySpriteBatch.setColor(Color.WHITE);

        // Draw entity name
        if(suppliedName != null) {
            BitmapFont use = rc.pickupFont;
            use.getData().setScale(0.5f);
            rc.globalGlyph.setText(use, suppliedName);

            float _x = startX + 1 + (length - rc.globalGlyph.width) * 0.5f;
            float _y = startY + rc.globalGlyph.height + 8;

            color.a = alpha;
            GradientFont.drawGradient(use, rc.arraySpriteBatch, suppliedName, _x, _y, color);

            use.getData().setScale(1.0f);
        }
    }

}