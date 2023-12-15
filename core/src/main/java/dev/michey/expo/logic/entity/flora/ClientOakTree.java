package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.animator.SquishAnimator2D;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleEmitter;

public class ClientOakTree extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private ContactAnimator contactAnimator;
    private FoliageAnimator foliageAnimator;
    private ParticleEmitter leafParticleEmitter;

    private int variant;
    private int leavesVariant;
    private boolean cut;
    private TextureRegion trunk;
    private Texture leaves;
    private TextureRegion trunkShadowMask;
    private TextureRegion leavesShadowMask;
    private float[] interactionPointArray;

    private float leavesDisplacement;
    private final float colorMix = MathUtils.random(0.125f);

    private float playerBehindDelta = 1.0f;
    private float playerBehindInterpolated = 1.0f;
    private float resetShadowFadeTimer;

    private TextureRegion selectionTrunk;

    public final SquishAnimator2D squishAnimator2D = new SquishAnimator2D(0.2f, 1.5f, 1.5f);

    /*
     *      [0] = LeavesWidth
     *      [1] = LeavesHeight
     *          [2] = TrunkWidth
     *          [3] = TrunkHeight
     *      [4] = TotalWidth
     *      [5] = TotalHeight
     *          [6] = LeavesOffsetX
     *          [7] = LeavesOffsetY
     *      [8] = InteractionOffsetX
     *      [9] = InteractionOffsetY
     *          [10] = InteractionWidth
     *          [11] = InteractionHeight
     */
    public static final float[] LEAVES_SMALL = new float[] {46, 55};
    public static final float[] LEAVES_MEDIUM = new float[] {55, 72};

    public static final float[][] MATRIX = new float[][] {
        new float[] {
                LEAVES_SMALL[0], LEAVES_SMALL[1],
                14, 40,
                46, 84,
                17, 29,
                3, 2,
                8, 6
        },
        new float[] {
                LEAVES_SMALL[0], LEAVES_SMALL[1],
                15, 41,
                46, 87,
                16, 32,
                4, 2,
                9, 6
        },
        new float[] {
                LEAVES_MEDIUM[0], LEAVES_MEDIUM[1],
                15, 41,
                55, 106,
                20, 34,
                4, 2,
                9, 6
        },
        new float[] {
                LEAVES_MEDIUM[0], LEAVES_MEDIUM[1],
                18, 70,
                55, 136,
                19, 64,
                5, 2,
                9, 6
        },
        new float[] {
                LEAVES_MEDIUM[0], LEAVES_MEDIUM[1],
                18, 119,
                55, 184,
                18, 112,
                5, 2,
                12, 6
        },
        new float[] {
                LEAVES_MEDIUM[0], LEAVES_MEDIUM[1],
                18, 96,
                55, 162,
                18, 90,
                5, 2,
                12, 6
        },
    };

    public static final float[][] CUT_MATRIX = new float[][] {
        new float[] {0, 0, 14, 17, 14, 17, 0, 0, 3, 2, 8, 6},
        new float[] {0, 0, 15, 17, 15, 17, 0, 0, 4, 2, 9, 6},
        new float[] {0, 0, 15, 17, 15, 17, 0, 0, 4, 2, 9, 6},
        new float[] {0, 0, 18, 17, 18, 17, 0, 0, 5, 2, 9, 6},
        new float[] {0, 0, 18, 17, 18, 17, 0, 0, 5, 2, 9, 6},
        new float[] {0, 0, 18, 17, 18, 17, 0, 0, 5, 2, 9, 6},
    };

    private float cutTotalWidth() {
        return CUT_MATRIX[variant - 1][4];
    }

    private float cutTotalHeight() {
        return CUT_MATRIX[variant - 1][5];
    }

    private float cutTrunkWidth() {
        return CUT_MATRIX[variant - 1][2];
    }

    private float cutTrunkHeight() {
        return CUT_MATRIX[variant - 1][3];
    }

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

    public float trunkWidth() {
        return MATRIX[variant - 1][2];
    }

    public float trunkHeight() {
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
        String cutStr = cut ? "cut_" : "";

        trunk = tr("eot_trunk_" + cutStr + variant);
        trunkShadowMask = tr("eot_trunk_" + cutStr + variant);
        selectionTrunk = generateSelectionTexture(trunk);

        leavesVariant = MathUtils.random() <= 0.25f ? 2 : 1;

        if(variant >= 1 && variant <= 2) {
            leaves = t("foliage/entity_oak_tree/leaves_oak_small_" + leavesVariant + ".png");
            leavesShadowMask = tr("leaves_oak_small_sm");
        } else {
            leaves = t("foliage/entity_oak_tree/leaves_oak_big_" + leavesVariant + ".png");
            leavesShadowMask = tr("leaves_oak_big_sm");
        }

        if(cut) {
            updateTextureBounds(cutTotalWidth(), cutTotalHeight(), 0, 0);
        } else {
            updateTextureBounds(totalWidth(), totalHeight() + leavesDisplacement, 0, 0);
        }

        interactionPointArray = new float[] {
            finalDrawPosX + interactionOffsetX(), finalDrawPosY + interactionOffsetY(),
            finalDrawPosX + interactionOffsetX() + interactionWidth(), finalDrawPosY + interactionOffsetY(),
            finalDrawPosX + interactionOffsetX(), finalDrawPosY + interactionOffsetY() + interactionHeight(),
            finalDrawPosX + interactionOffsetX() + interactionWidth(), finalDrawPosY + interactionOffsetY() + interactionHeight(),
        };

        updateDepth(5);

        if(!cut) {
            contactAnimator = new ContactAnimator(this);
            contactAnimator.small = false;
            contactAnimator.STRENGTH = 2.0f;
            contactAnimator.STRENGTH_DECREASE = 0.4f;

            foliageAnimator = new FoliageAnimator(0.5f, 1.2f, 0.01f, 0.02f, 0.03f, 0.04f, 2.0f, 5.0f, 0.5f, 1.5f);

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
                            .dynamicDepth(), 0.5f, 2.0f, 4.0f);
        }
    }

    @Override
    public void updateTexturePositionData() {
        if(cut) {
            finalDrawPosX = clientPosX - cutTrunkWidth() * 0.5f;
            finalDrawPosY = clientPosY;
            finalSelectionDrawPosX = clientPosX - cutTrunkWidth() * 0.5f - 1;   // Avoid when using Texture classes
            finalSelectionDrawPosY = finalDrawPosY - 1;                         // Avoid when using Texture classes

            finalTextureStartX = clientPosX - cutTrunkWidth() * 0.5f;
            finalTextureStartY = finalDrawPosY;

            finalTextureCenterX = finalTextureStartX + cutTotalWidth() * 0.5f;
            finalTextureCenterY = finalTextureStartY + cutTotalHeight() * 0.5f;
        } else {
            finalDrawPosX = clientPosX - trunkWidth() * 0.5f;
            finalDrawPosY = clientPosY;
            finalSelectionDrawPosX = clientPosX - trunkWidth() * 0.5f - 1;  // Avoid when using Texture classes
            finalSelectionDrawPosY = finalDrawPosY - 1;                     // Avoid when using Texture classes

            finalTextureStartX = clientPosX - trunkWidth() * 0.5f - leavesOffsetX();
            finalTextureStartY = finalDrawPosY;

            finalTextureCenterX = finalTextureStartX + totalWidth() * 0.5f;
            finalTextureCenterY = finalTextureStartY + totalHeight() * 0.5f;
        }

        finalTextureRootX = finalTextureCenterX;
        finalTextureRootY = finalTextureStartY;
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        if(newHealth <= 0) {
            playEntitySound("log_split");
            ParticleSheet.Common.spawnDustHitParticles(this);
        } else {
            playEntitySound("log_cut");
        }

        squishAnimator2D.reset();
        ParticleSheet.Common.spawnTreeHitParticles(this, clientPosX + 1.0f, finalTextureStartY + cutTrunkHeight() * 0.5f);

        if(GameSettings.get().enableParticles && !cut) {
            Color c = new Color((1.0f - colorMix), 1.0f, (1.0f - colorMix), 1.0f);

            new ParticleBuilder(ClientEntityType.PARTICLE_OAK_LEAF)
                    .amount(4, 8)
                    .scale(0.4f, 1.0f)
                    .lifetime(0.7f, 2.0f)
                    .position(finalTextureStartX + 8, clientPosY + leavesOffsetY())
                    .offset(leavesWidth() - 16, leavesHeight() * 0.25f)
                    .velocity(-8, 8, -8, -32)
                    .fadeout(0.25f)
                    .randomRotation()
                    .color(c)
                    .rotateWithVelocity()
                    .depth(depth + 0.01f)
                    .spawn();
        }

        if(!cut) {
            contactAnimator.onContact();
        }
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(!cut) {
            contactAnimator.tick(delta);
            foliageAnimator.resetWind();

            if(visibleToRenderEngine) {
                leafParticleEmitter.tick(delta);
            }

            ClientPlayer local = ClientPlayer.getLocalPlayer();
            boolean playerBehind;

            if(local != null && local.depth > depth) {
                playerBehind = ExpoShared.overlap(new float[] {
                        local.finalTextureStartX, local.finalTextureStartY,
                        local.finalTextureStartX + local.textureWidth, local.finalTextureStartY + local.textureHeight
                }, new float[] {
                        finalTextureStartX + foliageAnimator.value, finalTextureStartY + leavesOffsetY() + leavesDisplacement,
                        finalTextureStartX + leavesWidth() + foliageAnimator.value, finalTextureStartY + leavesOffsetY() + leavesDisplacement + leavesHeight()
                });
            } else {
                playerBehind = false;
            }

            float INTERPOLATION_SPEED_IN = 2.5f;
            float INTERPOLATION_SPEED_OUT = 1.25f;
            float MAX_TRANSPARENCY = 0.6f;

            if(playerBehind && playerBehindDelta > 0) {
                playerBehindDelta -= delta * INTERPOLATION_SPEED_IN;
                if(playerBehindDelta < 0) playerBehindDelta = 0;

                playerBehindInterpolated = 1f - MAX_TRANSPARENCY + Interpolation.smooth2.apply(playerBehindDelta) * MAX_TRANSPARENCY;
            }

            if(!playerBehind && playerBehindDelta < 1) {
                playerBehindDelta += delta * INTERPOLATION_SPEED_OUT;
                if(playerBehindDelta > 1) playerBehindDelta = 1;

                playerBehindInterpolated = 1f - MAX_TRANSPARENCY + Interpolation.smooth2.apply(playerBehindDelta) * MAX_TRANSPARENCY;
            }
        }
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        if(!cut) {
            foliageAnimator.calculateWindOnDemand();
        }
        setSelectionValues();
        squishAnimator2D.calculate(delta);

        rc.arraySpriteBatch.draw(selectionTrunk, finalSelectionDrawPosX - squishAnimator2D.squishX * 0.5f, finalSelectionDrawPosY,
                selectionTrunk.getRegionWidth() + squishAnimator2D.squishX, selectionTrunk.getRegionHeight() + squishAnimator2D.squishY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        if(!cut) {
            float dsp = (leaves.getWidth() - leavesWidth()) * 0.5f;
            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindInterpolated);
            rc.arraySpriteBatch.drawCustomVertices(leaves,
                    finalTextureStartX - dsp - squishAnimator2D.squishX * 0.5f,
                    finalTextureStartY + leavesOffsetY() + leavesDisplacement + squishAnimator2D.squishY * 0.5f,
                    leaves.getWidth() + squishAnimator2D.squishX, leaves.getHeight() + squishAnimator2D.squishY, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }

        /*
        if(!cut || resetShadowFadeTimer <= 0) {
            var meta = EntityMetadataMapper.get().getFor(getEntityType().ENTITY_SERVER_TYPE);

            if(cut) {
                drawHealthBar(rc, serverHealth / meta.getFloat("maxHp.trunk"), textureWidth, meta.getString("name.trunk"));
            } else {
                drawHealthBar(rc, (serverHealth - 30) / (meta.getFloat("maxHp.var" + variant) - 30), textureWidth, meta.getString("name.var" + variant));
            }
        }
        */
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            if(!cut) {
                foliageAnimator.calculateWindOnDemand();
            }
            squishAnimator2D.calculate(delta);
            updateDepth(5);
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.draw(trunk, finalDrawPosX - squishAnimator2D.squishX * 0.5f, finalDrawPosY,
                    trunk.getRegionWidth() + squishAnimator2D.squishX, trunk.getRegionHeight() + squishAnimator2D.squishY);

            if(!cut) {
                float dsp = (leaves.getWidth() - leavesWidth()) * 0.5f;
                rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindInterpolated);
                rc.arraySpriteBatch.drawCustomVertices(leaves,
                        finalTextureStartX - dsp - squishAnimator2D.squishX * 0.5f,
                        finalTextureStartY + leavesOffsetY() + leavesDisplacement + squishAnimator2D.squishY * 0.5f,
                        leaves.getWidth() + squishAnimator2D.squishX, leaves.getHeight() + squishAnimator2D.squishY, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
                rc.arraySpriteBatch.setColor(Color.WHITE);
            }
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(trunk, finalDrawPosX - squishAnimator2D.squishX * 0.5f, finalDrawPosY + 2, trunk.getRegionWidth() + squishAnimator2D.squishX, (trunk.getRegionHeight() + squishAnimator2D.squishY) * -1);

        if(!cut) {
            float dsp = (leaves.getWidth() - leavesWidth()) * 0.5f;
            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), 0.9f);
            rc.arraySpriteBatch.drawCustomVertices(leaves, finalTextureStartX - dsp - squishAnimator2D.squishX * 0.5f, finalTextureStartY - leavesOffsetY() - leavesDisplacement - squishAnimator2D.squishY * 0.5f + 2,
                    leaves.getWidth() + squishAnimator2D.squishX, (leaves.getHeight() + squishAnimator2D.squishY) * -1, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        if(resetShadowFadeTimer > 0) resetShadowFadeTimer -= delta;

        if(!cut) {
            foliageAnimator.calculateWindOnDemand();
        }
        Affine2 shadowT = ShadowUtils.createSimpleShadowAffine(finalDrawPosX - squishAnimator2D.squishX * 0.5f, finalDrawPosY);
        Affine2 shadowL = ShadowUtils.createSimpleShadowAffineInternalOffset(finalDrawPosX, finalDrawPosY, -leavesOffsetX() - squishAnimator2D.squishX * 0.5f, leavesOffsetY() + leavesDisplacement + squishAnimator2D.squishY * 0.5f);

        float[] trunkVertices = rc.arraySpriteBatch.obtainShadowVertices(trunkShadowMask, shadowT);
        boolean drawTrunk = rc.verticesInBounds(trunkVertices);

        boolean drawLeaves = !cut && rc.verticesInBounds(rc.arraySpriteBatch.obtainShadowVertices(leavesShadowMask, shadowL));

        if(drawTrunk || drawLeaves) {
            rc.useArrayBatch();
            float fraction = 1f / totalHeight();
            //float fraction = 1f / (totalHeight() + leavesDisplacement);

            if(drawTrunk) {
                float tt;
                float bt = 1.0f;

                if(cut) {
                    if(resetShadowFadeTimer <= 0) {
                        tt = fraction * cutTotalHeight();
                    } else {
                        tt = fraction * (totalHeight() - cutTotalHeight());
                    }
                } else {
                    tt = (totalHeight() - trunkHeight() + leavesDisplacement) * fraction;
                }

                float topColorT = new Color(0f, 0f, 0f, tt).toFloatBits();
                float bottomColorT = new Color(0f, 0f, 0f, bt).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(trunkShadowMask,
                        trunkShadowMask.getRegionWidth() + squishAnimator2D.squishX,
                        trunkShadowMask.getRegionHeight() + squishAnimator2D.squishY, shadowT, topColorT, bottomColorT);
            }

            if(drawLeaves) {
                float tl = 0f;
                float bl = fraction * leavesHeight();
                float topColorL = new Color(0f, 0f, 0f, tl).toFloatBits();
                float bottomColorL = new Color(0f, 0f, 0f, bl).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomVerticesCustomColor(leavesShadowMask,
                        leavesShadowMask.getRegionWidth(),
                        leavesShadowMask.getRegionHeight(), shadowL, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value, topColorL, bottomColorL);
            }
        }
    }

    @Override
    public void renderAO(RenderContext rc) {
        if(cut && resetShadowFadeTimer <= 0) {
            drawAO100(rc, 0.5f, 0.75f, 1, 3.5f);
        } else {
            drawAO50(rc, 1.0f, 1.25f, 1, 3.5f);
        }
    }

    private void spawnFallingTree(float fallingRemaining, boolean fallingDirectionRight) {
        ClientFallingTree fallingTree = new ClientFallingTree();

        fallingTree.clientPosX = clientPosX - 0.5f;
        fallingTree.clientPosY = clientPosY + 12.0f;
        fallingTree.depth = fallingTree.clientPosY - 12.001f;
        fallingTree.wakeupId = entityId;

        float baf = leavesWidth() / 128f;

        fallingTree.leavesDisplacement = leavesDisplacement;
        fallingTree.variant = variant;
        fallingTree.leavesVariant = leavesVariant;
        fallingTree.fallingRightDirection = fallingDirectionRight;
        fallingTree.animationDelta = ServerOakTree.FALLING_ANIMATION_DURATION - fallingRemaining;
        fallingTree.colorDisplacement = colorMix;
        fallingTree.windDisplacement = (foliageAnimator == null ? 0 : (foliageAnimator.value * baf)) + (contactAnimator == null ? 0 : (contactAnimator.value * baf));
        fallingTree.windDisplacementBase = fallingTree.windDisplacement;
        fallingTree.transparency = playerBehindInterpolated;
        fallingTree.inheritedSquishAnimator = squishAnimator2D;

        entityManager().addClientSideEntity(fallingTree);
    }

    public void wakeup() {
        cut = true;
        leafParticleEmitter = null;

        trunk = tr("eot_trunk_cut_" + variant);
        trunkShadowMask = tr("eot_trunk_cut_" + variant);
        selectionTrunk = generateSelectionTexture(trunk);
        updateTextureBounds(cutTotalWidth(), cutTotalHeight(), 0, 0);

        resetShadowFadeTimer = ServerOakTree.FALLING_ANIMATION_DURATION;
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        cut = (boolean) payload[1];
        variant = (int) payload[0];

        if(leavesVariant == 0) {
            leavesVariant = MathUtils.random() <= 0.25f ? 2 : 1;
        }

        if((boolean) payload[2]) {
            spawnFallingTree((float) payload[3], (boolean) payload[4]);
        }

        leavesDisplacement = (float) payload[5];
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        boolean isNowCut = (boolean) payload[0];

        if(isNowCut && !cut) {
            spawnFallingTree((float) payload[2], (boolean) payload[3]);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.OAK_TREE;
    }

}