package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.font.GradientFont;
import dev.michey.expo.render.visbility.TopVisibilityEntity;

public class ClientDamageIndicator extends ClientEntity implements TopVisibilityEntity {

    public float damageNumber;
    public Vector2 moveDir;
    public Color color;

    public static final Color PLAYER_COLOR = new Color(237f / 255f, 90f / 255f, 90f / 255f, 1.0f);
    public static final Color DEFAULT_COLOR = new Color(1.0f, 220f / 255f, 0.0f, 1.0f);

    private float lifetime = 1.0f;
    private float startX;
    private float startY;
    private float alpha = 1.0f;
    private float scale = 1.0f;

    private float SPEED;

    @Override
    public void onCreation() {
        updateDepth();
        startX = clientPosX;
        startY = clientPosY;
        SPEED = 1.3f + MathUtils.random(0.4f);
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        //float LIFETIME_SPEED = 1.5f;
        lifetime -= delta * SPEED;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
        } else {
            float _i = Interpolation.exp5Out.apply(1f - lifetime);
            float _s = MathUtils.sin(lifetime * MathUtils.PI2) * 3f * lifetime;
            clientPosX = startX + _s;
            clientPosY = startY + _i * 48;

            if(lifetime >= 0.8f) {
                alpha = 1f - (lifetime - 0.8f) * 5f;
            } else if(lifetime <= 0.4) {
                alpha = lifetime / 0.4f;
            } else {
                alpha = 1.0f;
            }

            if(lifetime >= 0.75f) {
                scale = 1f + ((lifetime - 0.75f) * 4);
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
        BitmapFont use = rc.damageFont;

        use.getData().setScale(scale * 0.4f);

        float raw = damageNumber;

        String s;

        if(raw < 1) {
            s = String.valueOf(Math.round(raw * 10d) / 10d);
        } else {
            s = String.valueOf((int) raw);
        }

        rc.globalGlyph.setText(use, s);

        color.a = alpha;
        GradientFont.drawGradient(use, rc.arraySpriteBatch, s, clientPosX - rc.globalGlyph.width * 0.5f, clientPosY, color);

        use.getData().setScale(1.0f);
    }

}
