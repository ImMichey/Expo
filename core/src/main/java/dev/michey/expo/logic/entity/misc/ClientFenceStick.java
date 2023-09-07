package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

import java.util.Arrays;

public class ClientFenceStick extends ClientEntity implements SelectableEntity, ReflectableEntity {

    private float[] interactionPointArray;

    public int fenceOrientation;

    public TextureRegion fenceTexture;
    public TextureRegion selectionTexture;
    public TextureRegion shadowTexture;

    private void updateFenceTexture() {
        disableTextureCentering = true;
        fenceTexture = tr("entity_fence_stick_" + (fenceOrientation + 1));
        shadowTexture = tr("entity_fence_stick_sm_" + (fenceOrientation + 1));
        updateTextureBounds(16, 20, 0, 0);
        interactionPointArray = generateFenceInteractionArray();
        selectionTexture = generateSelectionTexture(fenceTexture);
    }

    public static final float[][] INTERACTION_POINTS = new float[][] {
            new float[] {8, 2},
            new float[] {8, 2},
            new float[] {8, 2, 15, 2},
            new float[] {15, 2, 9, 5},
            new float[] {8, 2},
            new float[] {8, 2},
            new float[] {15, 2, 8, 1},
            new float[] {15, 2, 8, 1},
            new float[] {1, 2, 8, 2},
            new float[] {1, 2, 7, 5},
            new float[] {1, 2, 8, 2, 15, 2},
            new float[] {1, 2, 8, 2, 15, 2},
            new float[] {1, 2, 8, 1},
            new float[] {1, 2, 8, 1},
            new float[] {1, 2, 8, 1, 15, 2},
            new float[] {1, 2, 8, 1, 15, 2},
    };

    public static final float[][] SHADOW_OFFSETS = new float[][] {
            new float[] {6, 1},
            new float[] {7, 1},
            new float[] {6, 1},
            new float[] {7, 1},
            new float[] {7, 3},
            new float[] {7, 3},
            new float[] {7, 0},
            new float[] {6, 0},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 1},
            new float[] {0, 0},
            new float[] {0, 0},
            new float[] {0, 0},
            new float[] {0, 0},
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
        playEntitySound("wood_hit");

        ParticleSheet.Common.spawnWoodHitParticles(this);
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
        float[] offsets = SHADOW_OFFSETS[fenceOrientation];

        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX + offsets[0], finalTextureStartY + offsets[1]);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(shadowTexture, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradient(shadowTexture, shadowTexture.getRegionWidth(), shadowTexture.getRegionHeight(), shadow);
        }
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
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(fenceTexture, finalDrawPosX, finalDrawPosY, fenceTexture.getRegionWidth(), fenceTexture.getRegionHeight() * -1);
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
