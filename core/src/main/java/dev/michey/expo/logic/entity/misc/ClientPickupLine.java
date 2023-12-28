package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;

public class ClientPickupLine extends ClientEntity implements TopVisibilityEntity {

    private static final float SCALE_DURATION = 0.15f;
    private static final float MAX_LIFETIME = 1.33f;
    private static final float ALPHA_DURATION = 0.5f;
    private static final float FLOAT_SPEED = 40;

    private float lifetime = MAX_LIFETIME;

    public int id;
    public int amount;
    private ItemMapping mapping;

    @Override
    public void onCreation() {
        mapping = ItemMapper.get().getMapping(id);
    }

    @Override
    public void onDeletion() {

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
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        lifetime -= delta;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
            return;
        }

        float alpha = Math.min(lifetime, ALPHA_DURATION);

        clientPosY += delta * FLOAT_SPEED;
        updateDepth();

        BitmapFont f = rc.m5x7_border_all[0];
        String dn = amount + "x " + mapping.displayName;
        rc.globalGlyph.setText(f, dn);

        float scl = 1.0f;
        float GAP = 4;

        if((MAX_LIFETIME - lifetime) < SCALE_DURATION) {
            float norm = (MAX_LIFETIME - lifetime) / SCALE_DURATION;
            scl = Interpolation.pow3InInverse.apply(norm);
        }

        float iconW = mapping.uiRender[0].useWidth;
        float iconH = mapping.uiRender[0].useHeight;
        float totalW = (rc.globalGlyph.width + GAP + iconW) * scl;
        float maxH = Math.max(iconH, rc.globalGlyph.height) * scl;

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, alpha / ALPHA_DURATION);

        for(ItemRender ir : mapping.uiRender) {
            rc.arraySpriteBatch.draw(ir.useTextureRegion, clientPosX + ir.offsetX * scl - totalW * 0.5f, clientPosY + ir.offsetY * scl + (maxH - iconH * scl) * 0.5f,
                    ir.useTextureRegion.getRegionWidth() * scl, ir.useTextureRegion.getRegionHeight() * scl);
        }

        f.setColor(mapping.color.r, mapping.color.g, mapping.color.b, alpha / ALPHA_DURATION);

        f.getData().setScale(scl);
        f.draw(rc.arraySpriteBatch, dn, clientPosX - totalW * 0.5f + (GAP + iconW) * scl, clientPosY + rc.globalGlyph.height * scl + (maxH - rc.globalGlyph.height * scl) * 0.5f);
        f.getData().setScale(1.0f);

        rc.arraySpriteBatch.setColor(Color.WHITE);
        f.setColor(Color.WHITE);
    }

}