package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ParticleBuilder;

public class ClientFallingTree extends ClientEntity implements ReflectableEntity {

    private TextureRegion treeTrunk;
    private TextureRegion treeLeaves;

    public boolean fallingRightDirection;
    public float leavesDisplacement;

    public float animationDelta;
    public int variant;
    private float rotation;
    public float colorDisplacement;
    public float windDisplacement;
    public float windDisplacementBase;
    public float transparency;

    public float windDisplacementAlpha;
    public float windDisplacementInterpolated;

    private final float PHASE_TOTAL_DURATION = 4.3f;

    private final float[] adjustmentValues = new float[6];
    public int wakeupId;

    @Override
    public void onCreation() {
        if(variant == 0) variant = 1;

        treeTrunk = tr("eot_falling_trunk_trim_" + variant);

        String lStr = "eot_falling_leaves";
        if(variant <= 2) lStr += "_smol";
        treeLeaves = tr(lStr);

        updateTextureBounds(treeLeaves);

        playEntitySound("falling_tree");

        // wake up parent
        ClientOakTree tree = (ClientOakTree) entityManager().getEntityById(wakeupId);
        if(tree != null) {
            tree.wakeup();
        }
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
            float SPEED = 0.5f;
            windDisplacementAlpha += delta * SPEED;
            if(windDisplacementAlpha >= 1.0f) {
                windDisplacementAlpha = 1.0f;
            }
            windDisplacementInterpolated = Interpolation.circle.apply(windDisplacementAlpha);
            windDisplacement = windDisplacementBase - windDisplacementBase * windDisplacementInterpolated;
        }

        if(transparency < 1) {
            transparency += delta * 0.5f;
        }

        updateAdjustmentValues();
    }

    @Override
    public void calculateReflection() {
        drawReflection = true;
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        float MAX_ROTATION = 100.0f;

        float negation = fallingRightDirection ? -1 : 1;
        float interpolated = Interpolation.exp10In.apply(animationDelta / PHASE_TOTAL_DURATION);
        rotation = (MAX_ROTATION) * negation * interpolated;

        rc.arraySpriteBatch.draw(treeTrunk, finalDrawPosX + adjustmentValues[0], finalDrawPosY - 22 - adjustmentValues[1], treeTrunk.getRegionWidth() * 0.5f, 0,
                treeTrunk.getRegionWidth(), treeTrunk.getRegionHeight(), 1.0f, -1.0f, -rotation);

        rc.arraySpriteBatch.setColor(1.0f - colorDisplacement, 1.0f, 1.0f - colorDisplacement, transparency);

        rc.arraySpriteBatch.drawCustomVertices(treeLeaves,
                finalDrawPosX + adjustmentValues[2] - adjustmentValues[5] * adjustmentValues[3],
                finalDrawPosY - 22 - adjustmentValues[4] * adjustmentValues[3],
                treeLeaves.getRegionWidth() * 0.5f, 0,
                treeLeaves.getRegionWidth(), treeLeaves.getRegionHeight(), 1.0f, -1.0f, -rotation, windDisplacement, windDisplacement);

        rc.arraySpriteBatch.setColor(Color.WHITE);
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

        rc.arraySpriteBatch.draw(treeTrunk, finalDrawPosX + adjustmentValues[0], finalDrawPosY + adjustmentValues[1], treeTrunk.getRegionWidth() * 0.5f, 0,
                treeTrunk.getRegionWidth(), treeTrunk.getRegionHeight(), 1.0f, 1.0f, rotation);

        rc.arraySpriteBatch.setColor(1.0f - colorDisplacement, 1.0f, 1.0f - colorDisplacement, transparency);

        rc.arraySpriteBatch.drawCustomVertices(treeLeaves,
                finalDrawPosX + adjustmentValues[2] - adjustmentValues[5] * adjustmentValues[3],
                finalDrawPosY + adjustmentValues[4] * adjustmentValues[3],
                treeLeaves.getRegionWidth() * 0.5f, 0,
                treeLeaves.getRegionWidth(), treeLeaves.getRegionHeight(), 1.0f, 1.0f, rotation, windDisplacement, windDisplacement);

        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    private void updateAdjustmentValues() {
        Vector2 disp = GenerationUtils.circular(rotation, 1);
        float dsp = 21f;

        float trunkX = 22f;
        float trunkY = 0f;

        float leavesX = 0f;

        if(variant == 1) {
            leavesX -= 0.5f;
            trunkX -= 5.5f;
        } else if(variant == 2) {
            trunkX -= 6.0f;
            trunkY += 1.0f;
            dsp += 4.0f;
            leavesX += 1.0f;
        } else if(variant == 3) {
            trunkX -= 0.5f;
            trunkY += 1.0f;

            leavesX += 0.5f;
            dsp += 4.0f;
        } else if(variant == 4) {
            trunkX -= 2.0f;

            dsp += 29.0f;
        } else if(variant == 6) {
            trunkX -= 2.0f;
            trunkY += 1.0f;

            leavesX -= 0.5f;
            dsp += 81f;
        } else if(variant == 7) {
            trunkX -= 2.0f;
            trunkY += 1.0f;

            leavesX -= 0.5f;
            dsp += 58f;
        }

        dsp += leavesDisplacement;

        adjustmentValues[0] = trunkX;
        adjustmentValues[1] = trunkY;
        adjustmentValues[2] = leavesX;
        adjustmentValues[3] = dsp;
        adjustmentValues[4] = disp.x;
        adjustmentValues[5] = disp.y;
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 trunkShadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(finalTextureStartX, finalTextureStartY - 12, adjustmentValues[0], 12 + adjustmentValues[1],
                treeTrunk.getRegionWidth() * 0.5f, 0, rotation);

        Affine2 leavesShadow = ShadowUtils.createSimpleShadowAffineInternalOffsetRotation(finalTextureStartX, finalTextureStartY - 12, adjustmentValues[2] - adjustmentValues[5] * adjustmentValues[3], 12 + adjustmentValues[4] * adjustmentValues[3],
                treeLeaves.getRegionWidth() * 0.5f, 0, rotation);

        rc.useArrayBatch();
        rc.useRegularArrayShader();

        float totalHeight = ClientOakTree.MATRIX[variant - 1][5];
        float fraction = 1f / totalHeight;
        float trunkDistanceFromGround = 12f + adjustmentValues[1];

        {
            // Trunk
            float b = 1f - trunkDistanceFromGround * fraction;
            float t = b - treeTrunk.getRegionHeight() * fraction;
            float bc = new Color(0, 0, 0, b).toFloatBits();
            float tc = new Color(0, 0, 0, t).toFloatBits();

            rc.arraySpriteBatch.drawGradientCustomColor(treeTrunk, treeTrunk.getRegionWidth(), treeTrunk.getRegionHeight(), trunkShadow, tc, bc);
        }

        {
            // Leaves
            float b = 0f + treeLeaves.getRegionHeight() * fraction;
            float t = 0f;
            float bc = new Color(0, 0, 0, b).toFloatBits();
            float tc = new Color(0, 0, 0, t).toFloatBits();

            rc.arraySpriteBatch.drawGradientCustomColor(treeLeaves, treeLeaves.getRegionWidth(), treeLeaves.getRegionHeight(), leavesShadow, tc, bc);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.FALLING_TREE;
    }

}