package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientBoulder extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private int variant;
    private TextureRegion texture;
    private TextureRegion ao;
    private TextureRegion shadowMask;
    private float[] interactionPointArray;
    private TextureRegion selectionTexture;

    @Override
    public void onCreation() {
        String texName = "entity_boulder";

        if(variant == 2) {
            texName += "_coal";
        }
        texture = tr(texName);
        ao = tr("entity_boulder_ao");
        shadowMask = texture;
        selectionTexture = generateSelectionTexture(texture);
        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray(2);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("stone_hit");

        if(selected && newHealth <= 0) {
            CameraShake.invoke(1.0f, 0.33f);
        }
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("stone_break");
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

        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth();
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
            rc.arraySpriteBatch.draw(ao, finalDrawPosX - 1, finalDrawPosY - 1);
            rc.arraySpriteBatch.drawGradient(shadowMask, textureWidth, textureHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BOULDER;
    }

}