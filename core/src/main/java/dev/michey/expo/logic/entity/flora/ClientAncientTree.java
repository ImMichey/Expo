package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ExpoShared;

public class ClientAncientTree extends ClientEntity implements SelectableEntity {

    private TextureRegion trunk;
    private Texture leaves;
    private TextureRegion trunkShadowMask;
    private TextureRegion leavesShadowMask;
    private float[] interactionPointArray;

    private final float colorMix = MathUtils.random(0.1f);

    private final float u_speed = MathUtils.random(0.6f, 1.5f);
    private final float u_offset = MathUtils.random(100f);
    private final float u_minStrength = MathUtils.random(0.02f, 0.05f);
    private final float u_maxStrength = MathUtils.random(0.055f, 0.07f);
    private final float u_interval = MathUtils.random(2.0f, 5.0f);
    private final float u_detail = MathUtils.random(0.5f, 1.5f);

    private float wind;
    private boolean calculatedWindThisTick = false;

    private float playerBehindDelta = 1.0f;

    @Override
    public void onCreation() {
        trunk = tr("entity_ancient_tree_trunk");
        leaves = t("foliage/entity_ancient_tree/entity_ancient_tree_leaves.png");

        trunkShadowMask = tr("entity_ancient_tree_trunk_shadow_mask");
        leavesShadowMask = tr("entity_ancient_tree_leaves_shadow_mask");

        updateTextureBounds(88, 207, -32, 0);
        interactionPointArray = new float[] {
                clientPosX + 2, clientPosY + 3,
                clientPosX + 20, clientPosY + 3,
                clientPosX + 20, clientPosY + 8,
                clientPosX + 2, clientPosY + 8,
        };
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
            playerBehind = ExpoShared.overlap(new float[] {
                local.clientPosX, local.clientPosY,
                local.clientPosX + local.textureWidth, local.clientPosY + local.textureHeight
            }, new float[] {
                    finalTextureStartX + wind, clientPosY + 84,
                    finalTextureStartX + 88 + wind, clientPosY + 84 + 124
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
        setSelectionValues(Color.BLACK);

        rc.arraySpriteBatch.draw(trunk, clientPosX, clientPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindDelta);
        float wind = ShadowUtils.getWind(u_maxStrength, u_minStrength, rc.deltaTotal * u_speed + u_offset, u_interval, u_detail);
        rc.arraySpriteBatch.drawCustomVertices(leaves, clientPosX - 32, clientPosY + 83, leaves.getWidth(), leaves.getHeight(), wind, wind);
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
            updateDepth(6);
            rc.useArrayBatch();

            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

            rc.arraySpriteBatch.draw(trunk, clientPosX, clientPosY);
            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindDelta);
            rc.arraySpriteBatch.drawCustomVertices(leaves, clientPosX - 32, clientPosY + 83, leaves.getWidth(), leaves.getHeight(), wind, wind);
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
        Affine2 shadowL = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX, clientPosY, -32, 83);

        float[] trunkVertices = rc.arraySpriteBatch.obtainShadowVertices(trunkShadowMask, shadowT);
        boolean drawTrunk = rc.verticesInBounds(trunkVertices);
        float[] leavesVertices = rc.arraySpriteBatch.obtainShadowVertices(leavesShadowMask, shadowL);
        boolean drawLeaves = rc.verticesInBounds(leavesVertices);

        if(drawTrunk || drawLeaves) {
            rc.useArrayBatch();
            float fraction = 1f / (207f);

            if(drawTrunk) {
                float tt = fraction * (111f); // 207-96=111
                float bt = 1.0f;
                float topColorT = new Color(0f, 0f, 0f, tt).toFloatBits();
                float bottomColorT = new Color(0f, 0f, 0f, bt).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(trunkShadowMask, trunkShadowMask.getRegionWidth(), trunkShadowMask.getRegionHeight(), shadowT, topColorT, bottomColorT);
            }

            if(drawLeaves) {
                float tl = 0.0f;
                float bl = fraction * 124f; // 76f
                float topColorL = new Color(0f, 0f, 0f, tl).toFloatBits();
                float bottomColorL = new Color(0f, 0f, 0f, bl).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomVerticesCustomColor(leavesShadowMask, leavesShadowMask.getRegionWidth(), leavesShadowMask.getRegionHeight(), shadowL, wind, wind, topColorL, bottomColorL);
            }
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ANCIENT_TREE;
    }

}