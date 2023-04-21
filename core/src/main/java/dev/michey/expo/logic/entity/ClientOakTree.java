package dev.michey.expo.logic.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;

public class ClientOakTree extends ClientEntity implements SelectableEntity {

    private int variant;
    private TextureRegion trunk;
    private Texture leaves;
    private TextureRegion trunkShadowMask;
    private TextureRegion leavesShadowMask;
    private float[] interactionPointArray;

    private final float leavesDisplacement = 0;//-MathUtils.random(0, 2);//MathUtils.random(-4, 13);
    private final float colorMix = MathUtils.random(0.1f);

    private final float u_speed = MathUtils.random(0.5f, 1.2f);
    private final float u_offset = MathUtils.random(100f);
    private final float u_minStrength = MathUtils.random(0.04f, 0.07f);
    private final float u_maxStrength = MathUtils.random(0.08f, 0.09f);
    private final float u_interval = MathUtils.random(2.0f, 5.0f);
    private final float u_detail = MathUtils.random(0.5f, 1.5f);

    private float wind;
    private boolean calculatedWindThisTick = false;

    private float playerBehindDelta = 1.0f;

    // Format: {TotalHeight, xOffset, yOffset}
    public static final float[][] TREE_MATRIX = new float[][] {
        new float[] {109, -22, 33},
        new float[] {113, -21, 37},
        new float[] {113, -21, 37},
        new float[] {138, -20, 62},
        new float[] {164, -21, 88},
    };

    @Override
    public void onCreation() {
        trunk = tr("eot_trunk_" + variant);
        trunkShadowMask = tr("eot_trunk_" + variant);

        leaves = t("foliage/entity_oak_tree/eot_leaves.png");
        leavesShadowMask = tr("eot_leaves_sm");

        updateTexture(TREE_MATRIX[variant - 1][1], 0, 57, TREE_MATRIX[variant - 1][0] + leavesDisplacement);
        interactionPointArray = new float[] {
                clientPosX + 2, clientPosY + 3,
                clientPosX + 9, clientPosY + 3,
                clientPosX + 9, clientPosY + 8,
                clientPosX + 2, clientPosY + 8,
        };
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
        calculatedWindThisTick = false;

        ClientPlayer local = ClientPlayer.getLocalPlayer();
        boolean playerBehind;

        if(local != null) {
            int LEAVES_WIDTH = 57;
            int LEAVES_HEIGHT = 76;

            playerBehind = RenderContext.get().entityVerticesIntersecting(new float[] {
                local.clientPosX, local.clientPosY,
                local.clientPosX + local.drawWidth, local.clientPosY + local.drawHeight
            }, new float[] {
                    clientPosX + TREE_MATRIX[variant - 1][1] + wind, clientPosY + TREE_MATRIX[variant - 1][2] + leavesDisplacement,
                    clientPosX + TREE_MATRIX[variant - 1][1] + LEAVES_WIDTH + wind, clientPosY + TREE_MATRIX[variant - 1][2] + leavesDisplacement + LEAVES_HEIGHT
            });
        } else {
            playerBehind = false;
        }

        if(playerBehind && playerBehindDelta > 0.5f) {
            playerBehindDelta -= delta;
            if(playerBehindDelta < 0.5f) playerBehindDelta = 0.5f;
        }

        if(!playerBehind && playerBehindDelta < 1.0f) {
            playerBehindDelta += delta;
            if(playerBehindDelta > 1.0f) playerBehindDelta = 1.0f;
        }
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        calculateWindOnDemand(rc.deltaTotal);
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.draw(trunk, clientPosX, clientPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindDelta);
        float wind = ShadowUtils.getWind(u_maxStrength, u_minStrength, rc.deltaTotal * u_speed + u_offset, u_interval, u_detail);
        rc.arraySpriteBatch.drawCustomVertices(leaves, clientPosX + TREE_MATRIX[variant - 1][1], clientPosY + TREE_MATRIX[variant - 1][2] + leavesDisplacement, leaves.getWidth(), leaves.getHeight(), wind, wind);
        rc.arraySpriteBatch.setColor(Color.WHITE);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            calculateWindOnDemand(rc.deltaTotal);
            updateDepth(5);
            rc.useArrayBatch();

            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

            rc.arraySpriteBatch.draw(trunk, clientPosX, clientPosY);
            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindDelta);
            rc.arraySpriteBatch.drawCustomVertices(leaves, clientPosX + TREE_MATRIX[variant - 1][1], clientPosY + TREE_MATRIX[variant - 1][2] + leavesDisplacement, leaves.getWidth(), leaves.getHeight(), wind, wind);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    private void calculateWindOnDemand(float deltaTotal) {
        if(!calculatedWindThisTick) {
            calculatedWindThisTick = true;
            wind = ShadowUtils.getWind(u_maxStrength, u_minStrength, deltaTotal * u_speed + u_offset, u_interval, u_detail);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        calculateWindOnDemand(rc.deltaTotal);
        Affine2 shadowT = ShadowUtils.createSimpleShadowAffine(clientPosX, clientPosY);
        Affine2 shadowL = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX, clientPosY, TREE_MATRIX[variant - 1][1], TREE_MATRIX[variant - 1][2] + leavesDisplacement);

        float[] trunkVertices = rc.arraySpriteBatch.obtainShadowVertices(trunkShadowMask, shadowT);
        boolean drawTrunk = rc.verticesInBounds(trunkVertices);
        float[] leavesVertices = rc.arraySpriteBatch.obtainShadowVertices(leavesShadowMask, shadowL);
        boolean drawLeaves = rc.verticesInBounds(leavesVertices);

        if(drawTrunk || drawLeaves) {
            rc.useArrayBatch();
            float fraction = 1f / (TREE_MATRIX[variant - 1][0] + leavesDisplacement);

            if(drawTrunk) {
                float tt = fraction * ((TREE_MATRIX[variant - 1][0] - trunk.getRegionHeight()) + leavesDisplacement); // 126-67=59
                float bt = 1.0f;
                float topColorT = new Color(0f, 0f, 0f, tt).toFloatBits();
                float bottomColorT = new Color(0f, 0f, 0f, bt).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(trunkShadowMask, trunkShadowMask.getRegionWidth(), trunkShadowMask.getRegionHeight(), shadowT, topColorT, bottomColorT);
            }

            if(drawLeaves) {
                float tl = 0.0f;
                float bl = fraction * 76f; // 76f
                float topColorL = new Color(0f, 0f, 0f, tl).toFloatBits();
                float bottomColorL = new Color(0f, 0f, 0f, bl).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomVerticesCustomColor(leavesShadowMask, leavesShadowMask.getRegionWidth(), leavesShadowMask.getRegionHeight(), shadowL, wind, wind, topColorL, bottomColorL);
            }
        }
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        variant = (int) payload[0];
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.OAK_TREE;
    }

}