package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;

public class ClientBlueberryBush extends ClientEntity implements SelectableEntity {

    private TextureRegion stem;
    private TextureRegion shadowMask;
    private Texture bush;
    private Texture bushFruits;
    private float[] interactionPointArray;

    private boolean hasBerries;

    @Override
    public void onCreation() {
        stem = tr("ebbb_stem");
        shadowMask = tr("ebbb_shadow_mask");

        bush = t("foliage/entity_blueberrybush/ebbb_crown.png");
        bushFruits = t("foliage/entity_blueberrybush/ebbb_crown_fruits.png");

        updateTexture(0, 0, shadowMask.getRegionWidth(), shadowMask.getRegionHeight());
        interactionPointArray = generateInteractionArray();
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        playEntitySound("grass_hit");
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.draw(stem, clientPosX + 1, clientPosY);
        rc.arraySpriteBatch.draw(hasBerries ? bushFruits : bush, clientPosX, clientPosY + 1);
        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(2);
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.draw(stem, clientPosX + 1, clientPosY);
            rc.arraySpriteBatch.draw(hasBerries ? bushFruits : bush, clientPosX, clientPosY + 1);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(shadowMask, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.useArrayBatch();
            rc.arraySpriteBatch.drawGradient(shadowMask, shadowMask.getRegionWidth(), shadowMask.getRegionHeight(), shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BLUEBERRY_BUSH;
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        hasBerries = (boolean) payload[0];
    }

    @Override
    public void readEntityDataUpdate(Object[] payload) {
        hasBerries = (boolean) payload[0];
    }

}