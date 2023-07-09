package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.flora.ServerFallingTree;

public class ClientFallingTree extends ClientEntity {

    private TextureRegion fallingTree;
    private boolean fallingRightDirection;
    private float animationDelta;
    private int variant;
    private float rotation;

    private final float PHASE_TOTAL_DURATION = 4.3f;
    private final float PHASE_1_DURATION = 3.0f;
    private final float PHASE_2_DURATION = PHASE_TOTAL_DURATION - PHASE_1_DURATION;

    @Override
    public void onCreation() {
        // eot_falling_ + variant
        fallingTree = tr("eot_falling_1");
        updateTextureBounds(fallingTree);

        playEntitySound("falling_tree");
    }

    @Override
    public void onDeletion() {
        CameraShake.invoke(5.0f, 0.6f);
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        animationDelta += delta;

        if(animationDelta >= ServerFallingTree.FALLING_ANIMATION_DURATION) {
            animationDelta = ServerFallingTree.FALLING_ANIMATION_DURATION;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = true;

        updateDepth(-12.001f);
        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float PHASE_1_ROTATION = 15.0f;
        float MAX_ROTATION = 100.0f;

        float negation = fallingRightDirection ? -1 : 1;
        float interpolated;

        if(animationDelta <= PHASE_1_DURATION) {
            interpolated = Interpolation.swingIn.apply(animationDelta / PHASE_1_DURATION);
            rotation = PHASE_1_ROTATION * interpolated * negation;
        } else if(animationDelta <= (PHASE_2_DURATION + PHASE_1_DURATION)) {
            interpolated = Interpolation.bounceOut.apply(Math.abs(PHASE_1_DURATION - animationDelta) / PHASE_2_DURATION);
            rotation = PHASE_1_ROTATION * negation + (MAX_ROTATION - PHASE_1_ROTATION) * negation * interpolated;
        } else {
            rotation = MAX_ROTATION * negation;
        }

        rc.arraySpriteBatch.draw(fallingTree, finalDrawPosX, finalDrawPosY, fallingTree.getRegionWidth() * 0.5f, 1,
                fallingTree.getRegionWidth(), fallingTree.getRegionHeight(), 1.0f, 1.0f, rotation);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(finalTextureStartX, finalTextureStartY - 12, 0, 12,
                fallingTree.getRegionWidth() * 0.5f, 1, rotation);

        rc.useArrayBatch();
        rc.useRegularArrayShader();
        rc.arraySpriteBatch.drawGradient(fallingTree, textureWidth, textureHeight, shadow);
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
        fallingRightDirection = (boolean) payload[1];
        animationDelta = (float) payload[2];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FALLING_TREE;
    }

}
