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
    private Texture bush;
    private Texture bushFruits;
    private TextureRegion shadow;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        stem = tr("entity_blueberrybush_stem");
        shadow = tr("entity_blueberrybush_shadowmask");
        bush = t("foliage/entity_blueberrybush/entity_blueberrybush_crown.png");
        bushFruits = t("foliage/entity_blueberrybush/entity_blueberrybush_fruits.png");

        // + 1, + 3
        updateTexture(0, 0, stem.getRegionWidth(), 15);
        interactionPointArray = generateInteractionArray();
    }

    @Override
    public void onDamage(float damage, float newHealth) {

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

        rc.arraySpriteBatch.draw(stem, clientPosX, clientPosY);
        rc.arraySpriteBatch.draw(bush, clientPosX + 1, clientPosY + 3);
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
            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.draw(stem, clientPosX, clientPosY);
            rc.arraySpriteBatch.draw(bush, clientPosX + 1, clientPosY + 3);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(this.shadow, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.useArrayBatch();
            rc.arraySpriteBatch.drawGradient(this.shadow, this.shadow.getRegionWidth(), this.shadow.getRegionHeight(), shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BLUEBERRY_BUSH;
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        
    }

}