package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;

public class ClientFenceStick extends ClientEntity implements SelectableEntity {

    private float[] interactionPointArray;

    public static final float[][] FENCE_TEXTURE_DATA = new float[][] {
            new float[] {6, 1},
            new float[] {7, 11},
            new float[] {6, 1},
            new float[] {7, 1},
            new float[] {7, 3},
            new float[] {7, 3},
            new float[] {7, 0},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 0},
            new float[] {0, 0},
            new float[] {0, 0},
            new float[] {0, 0},
    };

    public int fenceOrientation;

    public TextureRegion fenceTexture;
    public TextureRegion selectionTexture;

    private void updateFenceTexture() {
        disableTextureCentering = true;
        fenceTexture = tr("entity_fence_stick_" + (fenceOrientation + 1));
        updateTextureBounds(16, 20, 0, 0);
        interactionPointArray = generateInteractionArray();
        selectionTexture = generateSelectionTexture(fenceTexture);
    }

    @Override
    public void onCreation() {
        updateFenceTexture();
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        updateTexturePositionData();
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            updateDepth();

            rc.arraySpriteBatch.draw(fenceTexture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(fenceTexture);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FENCE_STICK;
    }

    @Override
    public void readEntityDataUpdate(Object[] payload) {
        fenceOrientation = (int) payload[0];
        updateFenceTexture();
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        fenceOrientation = (int) payload[0];
        updateFenceTexture();
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
    public float[] interactionPoints() {
        return interactionPointArray;
    }

}
