package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;

public class ClientDamageIndicator extends ClientEntity {

    public int damageNumber;
    public Vector2 moveDir;
    public TextureRegion[] damageNumbers;

    public float currentScale;
    public float scaleDelta;
    public float scaleAlpha;

    private final float MAX_LIFETIME = 0.5f;
    public float lifetime = MAX_LIFETIME;
    public float alpha;
    public float interpolatedAlpha;

    @Override
    public void onCreation() {
        String numberAsString = String.valueOf(damageNumber);
        damageNumbers = new TextureRegion[numberAsString.length()];

        for(int i = 0; i < numberAsString.length(); i++) {
            damageNumbers[i] = RenderContext.get().getNumber(Integer.parseInt(String.valueOf(numberAsString.charAt(i))));
        }

        updateDepth();
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        lifetime -= delta;

        if(lifetime <= 0) {
            entityManager().removeEntity(this);
            return;
        } else {
            clientPosX += -moveDir.x * delta * 24;
            clientPosY += -moveDir.y * delta * 24;
            clientPosY += delta * 32;

            float ALPHA_BOUNDS = 0.125f;
            float ALPHA_LOW_THRESHOLD = ALPHA_BOUNDS;
            float ALPHA_HIGH_THRESHOLD = MAX_LIFETIME - ALPHA_BOUNDS;

            if(lifetime >= ALPHA_HIGH_THRESHOLD) {
                alpha = Math.abs(lifetime - (ALPHA_HIGH_THRESHOLD + ALPHA_LOW_THRESHOLD)) / ALPHA_LOW_THRESHOLD;
            } else if(lifetime <= ALPHA_LOW_THRESHOLD) {
                alpha = lifetime / ALPHA_LOW_THRESHOLD;
            } else {
                alpha = 1.0f;
            }

            interpolatedAlpha = Interpolation.smooth2.apply(alpha);
        }

        float MIN_SCALE = 0.25f;
        float MAX_SCALE = 1.0f;

        scaleDelta += delta;
        scaleAlpha = Interpolation.smooth2.apply(scaleDelta);
        currentScale = MIN_SCALE + (Interpolation.pow4Out.apply(scaleAlpha / MAX_LIFETIME)) * (MAX_SCALE - MIN_SCALE);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float dx = 0;
        float tw = 0;

        float _r = 255f / 255f;
        float _g = 28f / 255f;
        float _b = 28f / 255f;
        rc.arraySpriteBatch.setColor(_r, _g, _b, interpolatedAlpha);

        for(TextureRegion n : damageNumbers) {
            tw += n.getRegionWidth() * currentScale;
        }

        for(TextureRegion n : damageNumbers) {
            float w = n.getRegionWidth() * currentScale;
            float h = n.getRegionHeight() * currentScale;

            rc.arraySpriteBatch.draw(n, clientPosX + dx - tw * 0.5f, clientPosY, w, h);

            dx += w;
        }

        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.DAMAGE_INDICATOR;
    }

}
