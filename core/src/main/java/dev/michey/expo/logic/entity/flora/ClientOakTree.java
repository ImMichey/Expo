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
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleEmitter;

public class ClientOakTree extends ClientEntity implements SelectableEntity {

    private final FoliageAnimator foliageAnimator = new FoliageAnimator(0.5f, 1.2f, 0.03f, 0.05f, 0.06f, 0.07f, 2.0f, 5.0f, 0.5f, 1.5f);
    private ParticleEmitter leafParticleEmitter;

    private int variant;
    private TextureRegion trunk;
    private Texture leaves;
    private TextureRegion trunkShadowMask;
    private TextureRegion leavesShadowMask;
    private float[] interactionPointArray;

    private final float leavesDisplacement = -MathUtils.random(0, 4);
    private final float colorMix = MathUtils.random(0.1f);

    private float playerBehindDelta = 1.0f;

    private TextureRegion selectionTrunk;

    /*
     *      [0] = LeavesWidth
     *      [1] = LeavesHeight
     *      [2] = TrunkWidth
     *      [3] = TrunkHeight
     *      [4] = TotalWidth
     *      [5] = TotalHeight
     *      [6] = LeavesOffsetX
     *      [7] = LeavesOffsetY
     *      [8] = InteractionOffsetX
     *      [9] = InteractionOffsetY
     *      [10] = InteractionWidth
     *      [11] = InteractionHeight
     */
    public static final float[][] MATRIX = new float[][] {
        new float[] {57, 76, 14, 40, 57, 109, 22, 33, 3, 2, 8, 6},
        new float[] {57, 76, 15, 41, 57, 113, 21, 37, 4, 2, 9, 6},
        new float[] {57, 76, 15, 41, 57, 113, 21, 37, 4, 2, 9, 6},
        new float[] {57, 76, 18, 70, 57, 138, 20, 62, 5, 2, 9, 6},
        new float[] {75, 101, 20, 95, 75, 191, 30, 90, 5, 2, 12, 6},
    };

    private float totalWidth() {
        return MATRIX[variant - 1][4];
    }

    private float totalHeight() {
        return MATRIX[variant - 1][5];
    }

    private float leavesWidth() {
        return MATRIX[variant - 1][0];
    }

    private float leavesHeight() {
        return MATRIX[variant - 1][1];
    }

    private float trunkWidth() {
        return MATRIX[variant - 1][2];
    }

    private float trunkHeight() {
        return MATRIX[variant - 1][3];
    }

    private float leavesOffsetX() {
        return MATRIX[variant - 1][6];
    }

    private float leavesOffsetY() {
        return MATRIX[variant - 1][7];
    }

    private float interactionWidth() {
        return MATRIX[variant - 1][10];
    }

    private float interactionHeight() {
        return MATRIX[variant - 1][11];
    }

    private float interactionOffsetX() {
        return MATRIX[variant - 1][8];
    }

    private float interactionOffsetY() {
        return MATRIX[variant - 1][9];
    }

    @Override
    public void onCreation() {
        trunk = tr("eot_trunk_" + variant);
        trunkShadowMask = tr("eot_trunk_" + variant);
        selectionTrunk = generateSelectionTexture(trunk);

        String large = variant == 5 ? "_big" : "";
        leaves = t("foliage/entity_oak_tree/eot_leaves" + large + ".png");
        leavesShadowMask = tr("eot_leaves" + large + "_sm");

        updateTextureBounds(totalWidth(), totalHeight() + leavesDisplacement, 0, 0);

        interactionPointArray = new float[] {
            finalDrawPosX + interactionOffsetX(), finalDrawPosY + interactionOffsetY(),
            finalDrawPosX + interactionOffsetX() + interactionWidth(), finalDrawPosY + interactionOffsetY(),
            finalDrawPosX + interactionOffsetX(), finalDrawPosY + interactionOffsetY() + interactionHeight(),
            finalDrawPosX + interactionOffsetX() + interactionWidth(), finalDrawPosY + interactionOffsetY() + interactionHeight(),
        };

        updateDepth(5);

        leafParticleEmitter = new ParticleEmitter(
                new ParticleBuilder(ClientEntityType.PARTICLE_OAK_LEAF)
                .amount(1)
                .scale(0.6f, 1.1f)
                .lifetime(3.0f, 6.0f)
                .position(clientPosX + positionOffsetX + totalWidth() * 0.1f, clientPosY + textureHeight - leavesHeight() - leavesHeight() * 0.1f)
                .offset(leavesWidth() * 0.8f, leavesHeight() * 0.6f)
                .velocity(-24, 24, -6, -20)
                .fadein(0.5f)
                .fadeout(0.5f)
                .randomRotation()
                .rotateWithVelocity()
                .depth(depth - 0.01f)
                .dynamicDepth(), 0.5f, 4.0f, 5.0f);
    }

    @Override
    public void updateTexturePositionData() {
        finalDrawPosX = clientPosX - trunkWidth() * 0.5f;
        finalDrawPosY = clientPosY;
        finalSelectionDrawPosX = clientPosX - trunkWidth() * 0.5f - 1;  // Avoid when using Texture classes
        finalSelectionDrawPosY = finalDrawPosY - 1;                     // Avoid when using Texture classes

        finalTextureStartX = clientPosX - trunkWidth() * 0.5f - leavesOffsetX();
        finalTextureStartY = finalDrawPosY;

        finalTextureCenterX = finalTextureStartX + totalWidth() * 0.5f;
        finalTextureCenterY = finalTextureStartY + totalHeight() * 0.5f;

        finalTextureRootX = finalTextureCenterX;
        finalTextureRootY = finalTextureStartY;
    }

    @Override
    public void onDamage(float damage, float newHealth) {
        playEntitySound("wood_cut");
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        foliageAnimator.resetWind();
        leafParticleEmitter.tick(delta);

        ClientPlayer local = ClientPlayer.getLocalPlayer();
        boolean playerBehind;

        if(local != null) {
            playerBehind = ExpoShared.overlap(new float[] {
                local.clientPosX, local.clientPosY,
                local.clientPosX + local.textureWidth, local.clientPosY + local.textureHeight
            }, new float[] {
                    finalTextureStartX + foliageAnimator.value, finalTextureStartY + leavesOffsetY() + leavesDisplacement,
                    finalTextureStartX + leavesWidth() + foliageAnimator.value, finalTextureStartY + leavesOffsetY() + leavesDisplacement + leavesHeight()
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
        foliageAnimator.calculateWindOnDemand();
        rc.bindAndSetSelection(rc.arraySpriteBatch);

        rc.arraySpriteBatch.draw(selectionTrunk, finalSelectionDrawPosX, finalSelectionDrawPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindDelta);
        rc.arraySpriteBatch.drawCustomVertices(leaves, finalTextureStartX, finalTextureStartY + leavesOffsetY() + leavesDisplacement, leaves.getWidth(), leaves.getHeight(), foliageAnimator.value, foliageAnimator.value);
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
            foliageAnimator.calculateWindOnDemand();
            updateDepth(5);
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.draw(trunk, finalDrawPosX, finalDrawPosY);

            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindDelta);
            rc.arraySpriteBatch.drawCustomVertices(leaves, finalTextureStartX, finalTextureStartY + leavesOffsetY() + leavesDisplacement, leaves.getWidth(), leaves.getHeight(), foliageAnimator.value, foliageAnimator.value);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        foliageAnimator.calculateWindOnDemand();
        Affine2 shadowT = ShadowUtils.createSimpleShadowAffine(finalDrawPosX, finalDrawPosY);
        Affine2 shadowL = ShadowUtils.createSimpleShadowAffineInternalOffset(finalDrawPosX, finalDrawPosY, -leavesOffsetX(), leavesOffsetY() + leavesDisplacement);

        float[] trunkVertices = rc.arraySpriteBatch.obtainShadowVertices(trunkShadowMask, shadowT);
        boolean drawTrunk = rc.verticesInBounds(trunkVertices);
        float[] leavesVertices = rc.arraySpriteBatch.obtainShadowVertices(leavesShadowMask, shadowL);
        boolean drawLeaves = rc.verticesInBounds(leavesVertices);

        if(drawTrunk || drawLeaves) {
            rc.useArrayBatch();
            float fraction = 1f / (totalHeight() + leavesDisplacement);

            if(drawTrunk) {
                float tt = fraction * (totalHeight() - trunkHeight() + leavesDisplacement);
                float bt = 1.0f;
                float topColorT = new Color(0f, 0f, 0f, tt).toFloatBits();
                float bottomColorT = new Color(0f, 0f, 0f, bt).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(trunkShadowMask, trunkShadowMask.getRegionWidth(), trunkShadowMask.getRegionHeight(), shadowT, topColorT, bottomColorT);
            }

            if(drawLeaves) {
                float tl = 0.0f;
                float bl = fraction * leavesHeight();
                float topColorL = new Color(0f, 0f, 0f, tl).toFloatBits();
                float bottomColorL = new Color(0f, 0f, 0f, bl).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomVerticesCustomColor(leavesShadowMask, leavesShadowMask.getRegionWidth(), leavesShadowMask.getRegionHeight(), shadowL, foliageAnimator.value, foliageAnimator.value, topColorL, bottomColorL);
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