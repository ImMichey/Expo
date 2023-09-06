package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

public class ClientLilypad extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private int variant;
    public TextureRegion texture;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;
    private float animationDelta = MathUtils.random(100f);
    public float animationSine;

    private float SPEED = 5.0f;
    private float STRENGTH = 1.0f;

    @Override
    public void onCreation() {
        texture = tr("entity_lilypad_" + variant);
        selectionTexture = generateSelectionTexture(texture);

        float w, h;

        if(variant == 1 || variant == 2) {
            w = 11;
            h = 9;
        } else {
            w = 8;
            h = 7;
        }

        updateTextureBounds(w, h, 0, 0);
        interactionPointArray = generateInteractionArray(2);

        SPEED = 3.0f + MathUtils.random(2.0f);
        STRENGTH = 0.5f + MathUtils.random(0.5f);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");

        new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                .amount(2, 5)
                .scale(0.6f, 0.9f)
                .lifetime(0.3f, 0.5f)
                .color(ParticleColorMap.random(1))
                .position(finalTextureCenterX, finalTextureCenterY)
                .velocity(-24, 24, -24, 24)
                .fadeout(0.15f)
                .textureRange(12, 14)
                .randomRotation()
                .rotateWithVelocity()
                .depth(depth - 0.0001f)
                .spawn();
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("harvest");
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        animationDelta += delta * SPEED;
        animationSine = MathUtils.sin(animationDelta) * STRENGTH;
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, Color.BLACK, false);

        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY + animationSine);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY + animationSine);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY + animationSine, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.LILYPAD;
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
    }

}