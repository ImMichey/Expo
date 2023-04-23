package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;

public class ClientBush extends ClientEntity implements SelectableEntity {

    private TextureRegion texture;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        texture = tr("entity_bush");

        updateTexture(0, 0, texture.getRegionWidth(), texture.getRegionHeight());
        interactionPointArray = new float[] {drawRootX, clientPosY + 3.0f};
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
        rc.useArrayBatch();
        if(rc.arraySpriteBatch.getShader() != rc.selectionShader) rc.arraySpriteBatch.setShader(rc.selectionShader);

        rc.arraySpriteBatch.draw(texture, clientPosX, clientPosY);

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

            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

            rc.arraySpriteBatch.draw(texture, clientPosX, clientPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.useArrayBatch();
            rc.arraySpriteBatch.drawGradient(texture, texture.getRegionWidth(), texture.getRegionHeight(), shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.BUSH;
    }

}