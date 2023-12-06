package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.visbility.TopVisibilityEntity;

public class ClientDamageIndicator extends ClientEntity implements TopVisibilityEntity {

    public int damageNumber;
    public Vector2 moveDir;
    public Color color;

    public static final Color PLAYER_COLOR = new Color(237f / 255f, 90f / 255f, 90f / 255f, 1.0f);
    public static final Color DEFAULT_COLOR = new Color(1.0f, 220f / 255f, 0.0f, 1.0f);

    private float lifetime = 1.0f;
    private float startX;
    private float startY;
    private float alpha = 1.0f;
    private float scale = 1.0f;

    @Override
    public void onCreation() {
        updateDepth();
        startX = clientPosX;
        startY = clientPosY;
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        float LIFETIME_SPEED = 1.5f;
        lifetime -= delta * LIFETIME_SPEED;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
        } else {
            float _i = Interpolation.exp5Out.apply(1f - lifetime);
            clientPosX = startX;
            clientPosY = startY + _i * 40;

            if(lifetime >= 0.75f) {
                alpha = 1.0f;
            } else {
                alpha = Interpolation.smooth.apply(lifetime * 4 / 3);
            }

            if(lifetime >= 0.75f) {
                scale = 1f + Interpolation.exp10Out.apply((lifetime - 0.75f) * 4) * 0.25f;
            } else {
                scale = 1f;
            }

            depth = clientPosY;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.DAMAGE_INDICATOR;
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        BitmapFont use = rc.m5x7_border_all[0];

        use.getData().setScale(scale);
        String s = String.valueOf(damageNumber);
        rc.globalGlyph.setText(use, s);

        use.setColor(color.r, color.g, color.b, alpha);
        use.draw(rc.arraySpriteBatch, s, clientPosX - rc.globalGlyph.width * 0.5f, clientPosY);
        use.setColor(Color.WHITE);

        use.getData().setScale(1.0f);
    }

}
