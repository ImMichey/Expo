package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.clientphysics.ClientPhysicsBody;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.animator.SimpleShakeAnimator;
import dev.michey.expo.render.animator.SquishAnimator2D;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleEmitter;

import static dev.michey.expo.server.main.logic.entity.flora.ServerOakTree.TREE_BODIES;

public class ClientOakTree extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private ContactAnimator contactAnimator;
    private FoliageAnimator foliageAnimator;
    private ParticleEmitter leafParticleEmitter;
    public final SquishAnimator2D squishAnimator2D = new SquishAnimator2D(0.2f, 1.5f, 1.5f);

    private ClientPhysicsBody physicsBody;

    private int trunkVariant;
    private boolean cut;
    private boolean emptyCrown;
    private float leavesOffset;

    private TextureRegion trunk, trunk_sel;
    private TextureRegion leaves;
    private float[] interactionPointArray;

    private final float colorMix = MathUtils.random(0.125f);

    private float playerBehindDelta = 1.0f;
    private float playerBehindInterpolated = 1.0f;
    private float resetShadowFadeTimer;

    private boolean drawTrunk, drawLeaves;

    private static final float BEEHIVE_SHAKE_SPEED = 7.5f;
    private static final float BEEHIVE_SHAKE_ROT = 30f;
    private static final float BEEHIVE_SWING_SPEED = 1.2f;
    private static final float BEEHIVE_SWING_ROT = 5f;
    private ServerOakTree.BeehiveData beehiveData;
    private TextureRegion beehiveTexture;
    private float beehiveDelta;
    private int beehiveSign = 1;
    private SimpleShakeAnimator beehiveShakeAnimator;

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
    public static final float[] LEAVES = new float[] {57, 74};
    public static final float[] TRUNK_VAR1 = new float[] {21, 47};
    public static final float[] TRUNK_VAR2 = new float[] {21, 66};

    public static final float[][] MATRIX = new float[][] {
            new float[] {
                    LEAVES[0], LEAVES[1],
                    TRUNK_VAR1[0], TRUNK_VAR1[1],
                    57, 109,
                    0, 35,
                    -5, 2,
                    10, 6
            },
            new float[] {
                    LEAVES[0], LEAVES[1],
                    TRUNK_VAR2[0], TRUNK_VAR2[1],
                    57, 128,
                    0, 54,
                    -5, 2,
                    10, 6
            },
            new float[] {
                    LEAVES[0], LEAVES[1],
                    TRUNK_VAR1[0], TRUNK_VAR1[1],
                    57, 109,
                    0, 35,
                    -5, 2,
                    10, 6
            },
            new float[] {
                    LEAVES[0], LEAVES[1],
                    TRUNK_VAR2[0], TRUNK_VAR2[1],
                    57, 128,
                    0, 54,
                    -5, 2,
                    10, 6
            },
            new float[] {
                    LEAVES[0], LEAVES[1],
                    TRUNK_VAR1[0], TRUNK_VAR1[1],
                    57, 109,
                    0, 35,
                    -5, 2,
                    10, 6
            },
            new float[] {
                    LEAVES[0], LEAVES[1],
                    TRUNK_VAR2[0], TRUNK_VAR2[1],
                    57, 128,
                    0, 54,
                    -5, 2,
                    10, 6
            },
    };

    public static final float[][] CUT_MATRIX = new float[][] {
        new float[] {0, 0, 21, 16, 21, 16, 0, 0, 3, 2, 8, 6},
        new float[] {0, 0, 21, 16, 21, 16, 0, 0, 3, 2, 8, 6},
        new float[] {0, 0, 21, 16, 21, 16, 0, 0, 3, 2, 8, 6},
        new float[] {0, 0, 21, 16, 21, 16, 0, 0, 3, 2, 8, 6},
        new float[] {0, 0, 21, 16, 21, 16, 0, 0, 3, 2, 8, 6},
        new float[] {0, 0, 21, 16, 21, 16, 0, 0, 3, 2, 8, 6},
    };

    private float cutTotalWidth() {
        return CUT_MATRIX[trunkVariant - 1][4];
    }

    private float cutTotalHeight() {
        return CUT_MATRIX[trunkVariant - 1][5];
    }

    private float cutTrunkWidth() {
        return CUT_MATRIX[trunkVariant - 1][2];
    }

    private float cutTrunkHeight() {
        return CUT_MATRIX[trunkVariant - 1][3];
    }

    private float totalWidth() {
        return MATRIX[trunkVariant - 1][4];
    }

    private float totalHeight() {
        return MATRIX[trunkVariant - 1][5];
    }

    private float leavesWidth() {
        return MATRIX[trunkVariant - 1][0];
    }

    private float leavesHeight() {
        return MATRIX[trunkVariant - 1][1];
    }

    public float trunkWidth() {
        return MATRIX[trunkVariant - 1][2];
    }

    public float trunkHeight() {
        return MATRIX[trunkVariant - 1][3];
    }

    private float leavesOffsetX() {
        return MATRIX[trunkVariant - 1][6];
    }

    private float leavesOffsetY() {
        return MATRIX[trunkVariant - 1][7];
    }

    private float interactionWidth() {
        return MATRIX[trunkVariant - 1][10];
    }

    private float interactionHeight() {
        return MATRIX[trunkVariant - 1][11];
    }

    private float interactionOffsetX() {
        return MATRIX[trunkVariant - 1][8];
    }

    private float interactionOffsetY() {
        return MATRIX[trunkVariant - 1][9];
    }

    @Override
    public void onCreation() {
        String cutStr = cut ? "cut_" : "";
        trunk = tr("oak_trunk_" + cutStr + trunkVariant);
        leaves = tr("oak_leaves_reg_" + trunkVariant);
        trunk_sel = generateSelectionTexture(trunk);

        if(cut) {
            updateTextureBounds(cutTotalWidth(), cutTotalHeight(), 0, 0);
        } else {
            updateTextureBounds(totalWidth(), totalHeight() + leavesOffset, -totalWidth() * 0.5f, 0);
        }

        interactionPointArray = new float[] {
                clientPosX + interactionOffsetX(), clientPosY + interactionOffsetY(),
                clientPosX + interactionOffsetX() + interactionWidth(), clientPosY + interactionOffsetY(),
                clientPosX + interactionOffsetX(), clientPosY + interactionOffsetY() + interactionHeight(),
                clientPosX + interactionOffsetX() + interactionWidth(), clientPosY + interactionOffsetY() + interactionHeight(),
        };

        updateDepth(4);

        float[] b = TREE_BODIES[trunkVariant - 1];
        physicsBody = new ClientPhysicsBody(this, b[0], b[1], b[2], b[3]);

        if(!cut && !emptyCrown) {
            contactAnimator = new ContactAnimator(this);
            contactAnimator.small = false;
            contactAnimator.STRENGTH = 2.0f;
            contactAnimator.STRENGTH_DECREASE = 0.4f;

            //foliageAnimator = new FoliageAnimator(this, 0.5f, 1.2f, 0.01f, 0.02f, 0.03f, 0.04f, 2.0f, 5.0f, 0.5f, 1.5f);
            foliageAnimator = new FoliageAnimator(this);

            Color c = new Color((1.0f - colorMix), 1.0f, (1.0f - colorMix), 1.0f);
            leafParticleEmitter = new ParticleEmitter(
                    new ParticleBuilder(ClientEntityType.PARTICLE_OAK_LEAF)
                            .amount(1)
                            .scale(0.6f, 1.1f)
                            .lifetime(3.0f, 6.0f)
                            .position(clientPosX - leavesWidth() * 0.45f, clientPosY + totalHeight() - leavesHeight() * 0.9f)
                            .offset(leavesWidth() * 0.8f, leavesHeight() * 0.6f)
                            .velocity(-24, 24, -6, -20)
                            .fadein(0.5f)
                            .fadeout(0.5f)
                            .randomRotation()
                            .color(c)
                            .rotateWithVelocity()
                            .depth(depth - 0.01f)
                            .dynamicDepth(), 0.5f, 2.0f, 4.0f);
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        if(newHealth <= 0) {
            playEntitySound("log_split");
            ParticleSheet.Common.spawnDustHitParticles(this);
        } else {
            playEntitySound("log_cut");
        }

        if(beehiveData != null) {
            beehiveShakeAnimator.reset(1.0f);
            ClientEntity sourceEntity = entityManager().getEntityById(damageSourceEntityId);

            if(sourceEntity != null) {
                beehiveShakeAnimator.contactDir = sourceEntity.serverDirX == 0 ? (sourceEntity.finalTextureCenterX < finalTextureCenterX ? -1 : 1) : (sourceEntity.serverDirX < 0 ? 1 : -1);
            }
        }

        squishAnimator2D.reset();
        ParticleSheet.Common.spawnTreeHitParticles(this, clientPosX + 1.0f, finalTextureStartY + cutTrunkHeight() * 0.5f);

        if(GameSettings.get().enableParticles && !cut && !emptyCrown) {
            Color c = new Color((1.0f - colorMix), 1.0f, (1.0f - colorMix), 1.0f);

            new ParticleBuilder(ClientEntityType.PARTICLE_OAK_LEAF)
                    .amount(4, 8)
                    .scale(0.4f, 1.0f)
                    .lifetime(0.7f, 2.0f)
                    .position(clientPosX - leavesWidth() * 0.5f + 8, clientPosY + leavesOffsetY())
                    .offset(leavesWidth() - 16, leavesHeight() * 0.25f)
                    .velocity(-8, 8, -8, -32)
                    .fadeout(0.25f)
                    .randomRotation()
                    .color(c)
                    .rotateWithVelocity()
                    .depth(depth + 0.01f)
                    .spawn();
        }

        if(!cut && !emptyCrown) {
            contactAnimator.onContact();
        }
    }

    @Override
    public void onDeletion() {
        physicsBody.dispose();
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(!cut && !emptyCrown) {
            foliageAnimator.resetWind();

            if(visibleToRenderEngine || drawTrunk || drawLeaves) {
                leafParticleEmitter.tick(delta);

                contactAnimator.tick(delta);

                ClientPlayer local = ClientPlayer.getLocalPlayer();
                boolean playerBehind;

                if(local != null && local.depth > depth) {
                    playerBehind = ExpoShared.overlap(new float[] {
                            local.finalTextureStartX, local.finalTextureStartY,
                            local.finalTextureStartX + local.textureWidth, local.finalTextureStartY + local.textureHeight
                    }, new float[] {
                            finalTextureStartX + foliageAnimator.value, finalTextureStartY + leavesOffsetY() + leavesOffset,
                            finalTextureStartX + leavesWidth() + foliageAnimator.value, finalTextureStartY + leavesOffsetY() + leavesOffset + leavesHeight()
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

        if(beehiveData != null) {
            beehiveDelta += delta * BEEHIVE_SWING_SPEED * beehiveSign;
            beehiveShakeAnimator.calculate(delta);

            if(beehiveDelta >= 1 && beehiveSign > 0) {
                // Negate.
                beehiveDelta = 1.0f;
                beehiveSign = -1;
            } else if(beehiveDelta <= 0 && beehiveSign < 0) {
                beehiveDelta = 0.0f;
                beehiveSign = 1;
            }
        }
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        if(!cut && !emptyCrown) {
            foliageAnimator.calculateWindOnDemand();
        }
        setSelectionValues();
        squishAnimator2D.calculate(delta);

        rc.arraySpriteBatch.draw(trunk_sel, clientPosX - 1 - trunkWidth() * 0.5f - squishAnimator2D.squishX * 0.5f, clientPosY - 1,
                trunk_sel.getRegionWidth() + squishAnimator2D.squishX, trunk_sel.getRegionHeight() + squishAnimator2D.squishY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        drawBeehive(rc);

        if(!cut && !emptyCrown) {
            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindInterpolated);
            rc.arraySpriteBatch.drawShiftedVertices(leaves,
                    finalDrawPosX - leavesWidth() * 0.5f - squishAnimator2D.squishX * 0.5f,
                    finalDrawPosY + leavesOffsetY() + leavesOffset + squishAnimator2D.squishY * 0.5f,
                    leaves.getRegionWidth() + squishAnimator2D.squishX, leaves.getRegionHeight() + squishAnimator2D.squishY, foliageAnimator.value + contactAnimator.value, 0);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this, 4);

        if(visibleToRenderEngine) {
            if(!cut && !emptyCrown) {
                foliageAnimator.calculateWindOnDemand();
            }
            squishAnimator2D.calculate(delta);
            updateDepth(4);
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.draw(trunk, clientPosX - trunkWidth() * 0.5f - squishAnimator2D.squishX * 0.5f, clientPosY,
                    trunk.getRegionWidth() + squishAnimator2D.squishX, trunk.getRegionHeight() + squishAnimator2D.squishY);

            drawBeehive(rc);

            if(!cut && !emptyCrown) {
                rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), playerBehindInterpolated);
                rc.arraySpriteBatch.drawShiftedVertices(leaves,
                        finalDrawPosX - leavesWidth() * 0.5f - squishAnimator2D.squishX * 0.5f,
                        finalDrawPosY + leavesOffsetY() + leavesOffset + squishAnimator2D.squishY * 0.5f,
                        leaves.getRegionWidth() + squishAnimator2D.squishX, leaves.getRegionHeight() + squishAnimator2D.squishY, foliageAnimator.value + contactAnimator.value, 0);
                rc.arraySpriteBatch.setColor(Color.WHITE);
            }
        }
    }

    private void drawBeehive(RenderContext rc) {
        if(beehiveData != null) {
            float bhx = finalDrawPosX + beehiveData.offsetX() - beehiveTexture.getRegionWidth() * 0.5f;
            float bhy = finalDrawPosY + trunkHeight() - beehiveTexture.getRegionHeight() + leavesOffset + beehiveData.offsetY();
            float rot = Interpolation.sine.apply(beehiveDelta) * BEEHIVE_SWING_ROT - BEEHIVE_SWING_ROT * 0.5f + beehiveShakeAnimator.value * 10f;

            rc.arraySpriteBatch.draw(beehiveTexture, bhx, bhy,
                    beehiveTexture.getRegionWidth() * 0.5f, beehiveTexture.getRegionHeight(),
                    beehiveTexture.getRegionWidth(), beehiveTexture.getRegionHeight(),
                    1.0f, 1.0f, rot);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(trunk, finalDrawPosX - trunkWidth() * 0.5f - squishAnimator2D.squishX * 0.5f, finalDrawPosY + 2, trunk.getRegionWidth() + squishAnimator2D.squishX, (trunk.getRegionHeight() + squishAnimator2D.squishY) * -1);

        if(!cut && !emptyCrown) {
            foliageAnimator.calculateWindOnDemand(true);
            rc.arraySpriteBatch.setColor((1.0f - colorMix), 1.0f, (1.0f - colorMix), 0.9f);
            rc.arraySpriteBatch.drawShiftedVertices(leaves, finalDrawPosX - leavesWidth() * 0.5f - squishAnimator2D.squishX * 0.5f, finalDrawPosY - leavesOffsetY() - leavesOffset - squishAnimator2D.squishY * 0.5f + 2,
                    leaves.getRegionWidth() + squishAnimator2D.squishX, (leaves.getRegionHeight() + squishAnimator2D.squishY) * -1, foliageAnimator.value + contactAnimator.value, 0);

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        if(resetShadowFadeTimer > 0) resetShadowFadeTimer -= delta;

        if(!cut && !emptyCrown) {
            foliageAnimator.calculateWindOnDemand();
        }
        Affine2 shadowT = ShadowUtils.createSimpleShadowAffine(clientPosX - trunkWidth() * 0.5f - squishAnimator2D.squishX * 0.5f, clientPosY);
        Affine2 shadowL = ShadowUtils.createSimpleShadowAffineInternalOffset(clientPosX - leavesWidth() * 0.5f, clientPosY,
                -leavesOffsetX() - squishAnimator2D.squishX * 0.5f, leavesOffsetY() + leavesOffset + squishAnimator2D.squishY * 0.5f);

        float[] trunkVertices = rc.arraySpriteBatch.obtainShadowVertices(trunk, shadowT);
        drawTrunk = rc.verticesInBounds(trunkVertices);
        drawLeaves = !cut && !emptyCrown && rc.verticesInBounds(rc.arraySpriteBatch.obtainShadowVertices(leaves, shadowL));

        if(drawTrunk || drawLeaves) {
            rc.useArrayBatch();
            float fraction = 1f / totalHeight();

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
                    if(emptyCrown) {
                        tt = 0f;
                    } else {
                        tt = (totalHeight() - trunkHeight() + leavesOffset) * fraction;
                    }
                }

                float topColorT = new Color(0f, 0f, 0f, tt).toFloatBits();
                float bottomColorT = new Color(0f, 0f, 0f, bt).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomColor(trunk,
                        trunk.getRegionWidth() + squishAnimator2D.squishX,
                        trunk.getRegionHeight() + squishAnimator2D.squishY, shadowT, topColorT, bottomColorT);
            }

            if(drawLeaves) {
                float tl = 0f;
                float bl = fraction * leavesHeight();
                float topColorL = new Color(0f, 0f, 0f, tl).toFloatBits();
                float bottomColorL = new Color(0f, 0f, 0f, bl).toFloatBits();

                rc.arraySpriteBatch.drawGradientCustomVerticesCustomColor(leaves,
                        leaves.getRegionWidth() + squishAnimator2D.squishX,
                        leaves.getRegionHeight() + squishAnimator2D.squishY, shadowL, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value, topColorL, bottomColorL);
            }
        }
    }

    @Override
    public void renderAO(RenderContext rc) {
        if(cut && resetShadowFadeTimer <= 0) {
            drawAO50(rc, 0.5f, 0.75f, 1, 3.5f);
        } else {
            drawAO50(rc, 1.0f, 1.25f, 1, 3.5f);
        }
    }

    private void spawnFallingTree(float fallingRemaining, boolean fallingDirectionRight) {
        ClientFallingTree fallingTree = new ClientFallingTree();

        fallingTree.clientPosX = clientPosX;
        fallingTree.clientPosY = clientPosY + 10.0f;
        fallingTree.depth = fallingTree.clientPosY - 10.001f;
        fallingTree.wakeupId = entityId;

        fallingTree.emptyCrown = emptyCrown;
        fallingTree.leavesDisplacement = leavesOffset;
        fallingTree.trunkVariant = trunkVariant;
        fallingTree.fallingRightDirection = fallingDirectionRight;
        fallingTree.animationDelta = ServerOakTree.FALLING_ANIMATION_DURATION - fallingRemaining;
        fallingTree.colorDisplacement = colorMix;
        fallingTree.windDisplacement = (foliageAnimator == null ? 0 : (foliageAnimator.value)) + (contactAnimator == null ? 0 : (contactAnimator.value));
        fallingTree.windDisplacementBase = fallingTree.windDisplacement;
        fallingTree.transparency = playerBehindInterpolated;
        fallingTree.inheritedSquishAnimator = squishAnimator2D;
        fallingTree.leavesOffsetY = leavesOffsetY();

        entityManager().addClientSideEntity(fallingTree);
    }

    public void wakeup() {
        cut = true;
        leafParticleEmitter = null;

        trunk = tr("oak_trunk_cut_" + trunkVariant);
        trunk_sel = generateSelectionTexture(trunk);
        updateTextureBounds(cutTotalWidth(), cutTotalHeight(), -cutTotalWidth() * 0.5f, 0);

        resetShadowFadeTimer = ServerOakTree.FALLING_ANIMATION_DURATION;
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        ServerOakTree.TreeData td = (ServerOakTree.TreeData) payload[0];

        trunkVariant = td.trunkVariant();
        cut = td.cut();
        leavesOffset = td.leavesOffset();
        emptyCrown = td.emptyCrown();

        if(td.falling()) {
            spawnFallingTree(td.fallingRemaining(), td.fallingDirectionRight());
        }

        if(payload.length > 1) {
            beehiveData = (ServerOakTree.BeehiveData) payload[1];
            beehiveTexture = tr("entity_beehive");
            beehiveDelta = MathUtils.random();
            beehiveShakeAnimator = new SimpleShakeAnimator();
        }
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        ServerOakTree.TreeData td = (ServerOakTree.TreeData) payload[0];

        if(td.cut() && !cut) {
            spawnFallingTree(td.fallingRemaining(), td.fallingDirectionRight());
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.OAK_TREE;
    }

}