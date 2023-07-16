package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.util.ParticleBuilder;

public class ClientFallingTree extends ClientEntity {

    private TextureRegion treeTrunk;
    private TextureRegion treeLeaves;

    public boolean fallingRightDirection;
    public float leavesDisplacement;

    public float animationDelta;
    public int variant;
    private float rotation;
    public float colorDisplacement;
    public float windDisplacement;
    public float windDisplacementPerTick;
    public float windDisplacementBase;
    public float transparency;

    private final float PHASE_TOTAL_DURATION = 4.3f;

    @Override
    public void onCreation() {
        if(variant == 0) variant = 1;

        treeTrunk = tr("eot_falling_TRUNK_" + variant);
        treeLeaves = tr("eot_falling_LEAVES_" + variant);

        updateTextureBounds(treeLeaves);

        playEntitySound("falling_tree");
    }

    @Override
    public void onDeletion() {
        CameraShake.invoke(5.0f, 0.6f);

        int reach = 90;
        int dir = fallingRightDirection ? 1 : -1;

        if(variant == 4) {
            reach = 120;
        }

        new ParticleBuilder(ClientEntityType.PARTICLE_OAK_LEAF)
                .amount(32, 64)
                .scale(0.6f, 1.1f)
                .lifetime(0.6f, 2.5f)
                .position(clientPosX + (20 * dir), clientPosY - 12)
                .offset(reach * dir, 0)
                .velocity(-32, 32, 24, 80)
                .fadeout(0.25f)
                .randomRotation()
                .rotateWithVelocity()
                .depth(depth - 0.01f)
                .spawn();
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        animationDelta += delta;

        if(animationDelta >= ServerOakTree.FALLING_ANIMATION_DURATION) {
            animationDelta = ServerOakTree.FALLING_ANIMATION_DURATION;
            entityManager().removeEntity(this);
        }

        if(windDisplacement != 0) {
            float SPEED = 3.0f;
            windDisplacementPerTick = windDisplacementBase * -1 / ServerOakTree.FALLING_ANIMATION_DURATION;
            windDisplacement += windDisplacementPerTick * delta * SPEED;

            if(windDisplacementPerTick > 0) {
                if(windDisplacement > 0) {
                    windDisplacement = 0;
                }
            } else {
                if(windDisplacement < 0) {
                    windDisplacement = 0;
                }
            }
        }

        if(transparency < 1) {
            transparency += delta * 0.5f;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = true;

        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float MAX_ROTATION = 100.0f;

        float negation = fallingRightDirection ? -1 : 1;
        float interpolated = Interpolation.exp10In.apply(animationDelta / PHASE_TOTAL_DURATION);
        rotation = (MAX_ROTATION) * negation * interpolated;

        float adjustmentX = 0;
        float adjustmentY = 0;

        if(variant == 2 || variant == 3) {
            adjustmentX = 0.5f;
            adjustmentY = 1.0f;
        }

        rc.arraySpriteBatch.draw(treeTrunk, finalDrawPosX + adjustmentX, finalDrawPosY + adjustmentY, treeTrunk.getRegionWidth() * 0.5f, 1,
                treeTrunk.getRegionWidth(), treeTrunk.getRegionHeight(), 1.0f, 1.0f, rotation);

        rc.arraySpriteBatch.setColor(1.0f - colorDisplacement, 1.0f, 1.0f - colorDisplacement, transparency);
        rc.arraySpriteBatch.drawCustomVertices(treeLeaves, finalDrawPosX, finalDrawPosY + leavesDisplacement, treeLeaves.getRegionWidth() * 0.5f, 4,
                treeLeaves.getRegionWidth(), treeLeaves.getRegionHeight(), 1.0f, 1.0f, rotation, 0, 0);
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        float adjustmentX = 0;
        float adjustmentY = 0;

        if(variant == 2 || variant == 3) {
            adjustmentX = 0.5f;
            adjustmentY = 1.0f;
        }

        Affine2 trunkShadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(finalTextureStartX, finalTextureStartY - 12, adjustmentX, 12 + adjustmentY,
                treeLeaves.getRegionWidth() * 0.5f, 1, rotation);

        rc.useArrayBatch();
        rc.useRegularArrayShader();

        Affine2 leavesShadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(finalTextureStartX, finalTextureStartY - 12, 0, 12 + leavesDisplacement,
                treeLeaves.getRegionWidth() * 0.5f, 4, rotation);

        rc.arraySpriteBatch.drawGradient(treeTrunk, textureWidth, textureHeight, trunkShadow);
        rc.arraySpriteBatch.drawGradient(treeLeaves, textureWidth, textureHeight, leavesShadow);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FALLING_TREE;
    }

}