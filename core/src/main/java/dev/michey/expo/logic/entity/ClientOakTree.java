package dev.michey.expo.logic.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.client.ExpoClient;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.EntityRemovalReason;

import java.util.Arrays;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.CHUNK_SIZE;

public class ClientOakTree extends ClientEntity implements SelectableEntity {

    private TextureRegion trunk;
    private Texture leaves;
    private TextureRegion trunkShadowMask;
    private TextureRegion leavesShadowMask;
    private TextureRegion trunkProximityShadow;
    private float[] interactionPointArray;

    private float shaderSpeed = MathUtils.random(1.0f, 3.0f);
    private float shaderStrength = MathUtils.random(0.03f, 0.06f);
    private float shaderOffset = MathUtils.random(4.0f);

    @Override
    public void onCreation() {
        trunk = tr("tree_trunk");
        leaves = t("foliage/tree_leaves.png");
        trunkShadowMask = tr("tree_trunk_shadow_mask");
        leavesShadowMask = tr("tree_leaves_shadow_mask");
        trunkProximityShadow = tr("tree_trunk_proximity_shadow");

        updateTexture(0, 0, 67, 97);
        interactionPointArray = new float[] {
                clientPosX + 31, clientPosY + 7,
                clientPosX + 39, clientPosY + 7,
                clientPosX + 31, clientPosY + 10,
                clientPosX + 39, clientPosY + 10,
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
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        rc.useArrayBatch();
        if(rc.arraySpriteBatch.getShader() != rc.selectionShader) rc.arraySpriteBatch.setShader(rc.selectionShader);

        rc.arraySpriteBatch.draw(trunk, clientPosX + 21, clientPosY + 1);

        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        rc.arraySpriteBatch.draw(trunkProximityShadow, clientPosX + 21, clientPosY);
        rc.arraySpriteBatch.draw(leaves, clientPosX, clientPosY + 36);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(6);
            rc.useArrayBatch();

            if(rc.arraySpriteBatch.getShader() != rc.DEFAULT_GLES3_ARRAY_SHADER) rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

            rc.arraySpriteBatch.draw(trunk, clientPosX + 21, clientPosY + 1);
            rc.arraySpriteBatch.draw(trunkProximityShadow, clientPosX + 21, clientPosY);
            rc.arraySpriteBatch.draw(leaves, clientPosX, clientPosY + 36);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadowT = ShadowUtils.createSimpleShadowAffine(clientPosX + 23, clientPosY + 2);
        Affine2 shadowL = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX, clientPosY + 2, 0, 34);

        float[] trunkVertices = rc.arraySpriteBatch.obtainShadowVertices(trunkShadowMask, shadowT);
        boolean drawTrunk = rc.verticesInBounds(trunkVertices);
        float[] leavesVertices = rc.arraySpriteBatch.obtainShadowVertices(leavesShadowMask, shadowL);
        boolean drawLeaves = rc.verticesInBounds(leavesVertices);

        if(drawTrunk || drawLeaves) {
            rc.useArrayBatch();
            float fraction = 1f / 95f;

            if(drawTrunk) {
                float tt = fraction * 52f; // 52px
                float bt = 1.0f;
                float topColorT = new Color(0f, 0f, 0f, tt).toFloatBits();
                float bottomColorT = new Color(0f, 0f, 0f, bt).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(trunkShadowMask, trunkShadowMask.getRegionWidth(), trunkShadowMask.getRegionHeight(), shadowT, topColorT, bottomColorT);
            }

            if(drawLeaves) {
                float tl = 0.0f;
                float bl = fraction * 62f; // 62px
                float topColorL = new Color(0f, 0f, 0f, tl).toFloatBits();
                float bottomColorL = new Color(0f, 0f, 0f, bl).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(leavesShadowMask, leavesShadowMask.getRegionWidth(), leavesShadowMask.getRegionHeight(), shadowL, topColorL, bottomColorL);
            }
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.OAK_TREE;
    }

}