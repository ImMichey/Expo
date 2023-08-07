package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

import java.util.Arrays;

public class ClientFenceStick extends ClientEntity implements SelectableEntity {

    private float[] interactionPointArray;

    public int fenceOrientation;

    public TextureRegion fenceTexture;
    public TextureRegion selectionTexture;

    private void updateFenceTexture() {
        disableTextureCentering = true;
        fenceTexture = tr("entity_fence_stick_" + (fenceOrientation + 1));
        updateTextureBounds(16, 20, 0, 0);
        interactionPointArray = generateFenceInteractionArray();
        selectionTexture = generateSelectionTexture(fenceTexture);
    }

    public static final float[][] INTERACTION_POINTS = new float[][] {
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
            new float[] {8, 8},
    };

    private float[] generateFenceInteractionArray() {
        float[] pts = INTERACTION_POINTS[fenceOrientation];
        float[] interactions = Arrays.copyOf(pts, pts.length);

        for(int i = 0; i < interactions.length; i += 2) {
            interactions[i] += finalTextureStartX;
            interactions[i + 1] += finalTextureStartY;
        }

        return interactions;
    }

    @Override
    public void onCreation() {
        updateFenceTexture();
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("wood_cut");

        new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                .amount(3, 7)
                .scale(0.6f, 0.8f)
                .lifetime(0.3f, 0.5f)
                .color(ParticleColorMap.random(6))
                .position(finalTextureStartX + 8, finalTextureStartY + 8)
                .velocity(-24, 24, -24, 24)
                .fadeout(0.15f)
                .textureRange(12, 14)
                .randomRotation()
                .rotateWithVelocity()
                .depth(depth - 0.0001f)
                .spawn();
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
