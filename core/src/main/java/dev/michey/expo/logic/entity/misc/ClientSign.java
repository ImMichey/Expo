package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.render.visbility.TopVisibilityEntity;

public class ClientSign extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity, TopVisibilityEntity {

    private TextureRegion texture;
    private TextureRegion shadowMask;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;

    private TextureRegion square;

    public String text;

    private float fadeDelta;
    private float fadeAlpha;

    @Override
    public void onCreation() {
        texture = tr("entity_sign");
        shadowMask = tr("entity_sign");
        square = tr("square16x16");
        selectionTexture = generateSelectionTexture(texture);
        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray();
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(shadowMask, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(shadowMask, textureWidth, textureHeight, shadow);
        }
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        text = (String) payload[0];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SIGN;
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAOAuto50(rc);
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        ClientPlayer cp = ClientPlayer.getLocalPlayer();
        if(cp == null) return;

        float dst = Vector2.dst(cp.clientPosX, cp.clientPosY, clientPosX, clientPosY);
        float SPD = 3.0f;
        Interpolation anim = Interpolation.exp5Out;

        if(dst <= 64.0f) {
            fadeDelta += delta * SPD;

            if(fadeDelta > 1.0f) {
                fadeDelta = 1.0f;
            }
        } else {
            fadeDelta -= delta * SPD;

            if(fadeDelta < 0.0f) {
                fadeDelta = 0.0f;
            }
        }

        fadeAlpha = anim.apply(fadeDelta);

        if(fadeAlpha > 0) {
            BitmapFont use = rc.m5x7_border_all[0];
            rc.globalGlyph.setText(use, text);

            float sp = 3f;
            rc.arraySpriteBatch.setColor(0, 0, 0, fadeAlpha * 0.25f);
            rc.arraySpriteBatch.draw(square, clientPosX - rc.globalGlyph.width * 0.5f - sp, clientPosY + 26 - sp, rc.globalGlyph.width + sp * 2, rc.globalGlyph.height + sp * 2);
            rc.arraySpriteBatch.setColor(Color.WHITE);

            use.setColor(1.0f, 1.0f, 1.0f, fadeAlpha);
            use.draw(rc.arraySpriteBatch, text, clientPosX - rc.globalGlyph.width * 0.5f, clientPosY + 26 + rc.globalGlyph.height);
            use.setColor(Color.WHITE);
        }
    }

}