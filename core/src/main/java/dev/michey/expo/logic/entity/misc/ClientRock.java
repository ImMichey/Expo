package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientRock extends ClientEntity implements SelectableEntity {

    private int variant;
    private TextureRegion texture;
    private TextureRegion shadowMask;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        texture = ExpoAssets.get().textureRegion("entity_rock_" + variant);
        shadowMask = ExpoAssets.get().textureRegion("entity_rock_" + variant);
        updateTexture(0, 0, texture.getRegionWidth(), texture.getRegionHeight());
        interactionPointArray = new float[] {
                clientPosX + 2, clientPosY + 2,
                clientPosX + drawWidth - 2, clientPosY + 2,
                clientPosX + drawWidth - 2, clientPosY + drawHeight - 2,
                clientPosX + 2, clientPosY + drawHeight - 2,
        };
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        if(variant == 2) {
            playEntitySound("stone_hit");
        }
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            if(variant == 2) playEntitySound("stone_break");
        }
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
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
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.draw(texture, clientPosX, clientPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(2);
            rc.useArrayBatch();
            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
            rc.arraySpriteBatch.draw(texture, clientPosX, clientPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(shadowMask, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.useArrayBatch();
            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
            rc.arraySpriteBatch.drawGradient(shadowMask, drawWidth, drawHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ROCK;
    }

}