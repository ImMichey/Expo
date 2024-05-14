package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.clientphysics.ClientPhysicsBody;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.font.GradientFont;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;

import static dev.michey.expo.util.ClientUtils.darker;

public class ClientPickupLine extends ClientEntity implements TopVisibilityEntity {

    private static final float SCALE_DURATION = 0.25f;
    private static final float MAX_LIFETIME = 1.5f;
    private static final float ALPHA_DURATION = 0.75f;
    private static final float FLOAT_SPEED = 40;

    private float lifetime = MAX_LIFETIME;

    public int id;
    public int amount;
    private ItemMapping mapping;
    public String displayText;

    public Color topColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public Color bottomColor;

    private ClientPhysicsBody physicsBody;

    @Override
    public void onCreation() {
        RenderContext rc = RenderContext.get();
        BitmapFont f = rc.pickupFont;
        rc.globalGlyph.setText(f, displayText);
        float iconW = mapping.uiRender[0].useWidth;
        float iconH = mapping.uiRender[0].useHeight;
        float GAP = 4;
        float totalW = (rc.globalGlyph.width * 0.5f + GAP + iconW);
        float maxH = Math.max(iconH, rc.globalGlyph.height * 0.5f);

        physicsBody = new ClientPhysicsBody(this, -totalW * 0.5f - 3, -3f, totalW + 6, maxH + 6);
    }

    public void setMapping() {
        mapping = ItemMapper.get().getMapping(id);

        topColor = mapping.color;
        bottomColor = darker(topColor);
    }

    public void setCustomDisplayText(String text) {
        displayText = text;
    }

    public void setCustomDisplayColor(Color color) {
        topColor = color;
        bottomColor = darker(topColor);
    }

    @Override
    public void onDeletion() {
        if(physicsBody != null) physicsBody.dispose();
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.PICKUP_LINE;
    }

    public void reset() {
        lifetime = MAX_LIFETIME;
        ClientPlayer player = ClientPlayer.getLocalPlayer();
        clientPosX = player.clientPosX;
        clientPosY = player.clientPosY + player.textureHeight + 24;

        displayText = amount + "x " + mapping.displayName;

        if(physicsBody != null) {
            physicsBody.teleport(clientPosX, clientPosY);
        }
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        lifetime -= delta;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
            return;
        }

        float alpha = Math.min(lifetime, ALPHA_DURATION);

        if(alpha <= 0.25f) {
            if(physicsBody != null) {
                physicsBody.dispose();
                physicsBody = null;
            }
        }

        if(physicsBody != null) {
            var result = physicsBody.move(0, delta * FLOAT_SPEED, ClientPhysicsBody.pickupCollisionFilter);
            clientPosX = result.goalX - physicsBody.xOffset;
            clientPosY = result.goalY - physicsBody.yOffset;
        } else {
            clientPosY += delta * FLOAT_SPEED;
        }

        updateDepth();

        BitmapFont f = rc.pickupFont;
        rc.globalGlyph.setText(f, displayText);

        float scl = 1.0f;
        float GAP = 4;

        if((MAX_LIFETIME - lifetime) < SCALE_DURATION) {
            float norm = (MAX_LIFETIME - lifetime) / SCALE_DURATION;
            scl = Interpolation.pow3InInverse.apply(norm);
        }

        float fontScale = 0.5f;

        float iconW = mapping.uiRender[0].useWidth;
        float iconH = mapping.uiRender[0].useHeight;
        float totalW = (rc.globalGlyph.width * fontScale + GAP + iconW) * scl;
        float maxH = Math.max(iconH, rc.globalGlyph.height * fontScale) * scl;

        rc.arraySpriteBatch.setColor(0.0f, 0.0f, 0.0f, alpha / ALPHA_DURATION * 0.25f);
        rc.drawSquareRoundedDoubleAb(clientPosX - totalW * 0.5f - 2.5f, clientPosY - 2.5f, totalW + 5f, maxH + 5f);

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, alpha / ALPHA_DURATION);

        for(ItemRender ir : mapping.uiRender) {
            rc.arraySpriteBatch.draw(ir.useTextureRegion, clientPosX + ir.offsetX * scl - totalW * 0.5f, clientPosY + ir.offsetY * scl + (maxH - iconH * scl) * 0.5f,
                    ir.useTextureRegion.getRegionWidth() * scl, ir.useTextureRegion.getRegionHeight() * scl);
        }

        f.getData().setScale(scl * fontScale);

        topColor.a = alpha / ALPHA_DURATION;
        bottomColor.a = alpha / ALPHA_DURATION;

        GradientFont.drawGradient(f, rc.arraySpriteBatch, displayText,
                clientPosX - totalW * 0.5f + (GAP + iconW) * scl, clientPosY + rc.globalGlyph.height * fontScale * scl + (maxH - rc.globalGlyph.height * fontScale * scl) * 0.5f,
                topColor, bottomColor);

        topColor.a = 1.0f;
        bottomColor.a = 1.0f;

        f.setColor(Color.WHITE);
        f.getData().setScale(1.0f);
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

}