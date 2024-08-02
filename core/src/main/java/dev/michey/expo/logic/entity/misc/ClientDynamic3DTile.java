package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.clientphysics.ClientPhysicsBody;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.SquishAnimatorAdvanced2D;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;

public class ClientDynamic3DTile extends ClientEntity implements SelectableEntity, ReflectableEntity {

    public TileLayerType emulatingType;
    public int[] layerIds;
    private float[] interactionPointArray;

    private float playerBehindDelta;
    private float playerBehindDeltaInterpolated;

    public final SquishAnimatorAdvanced2D squishAnimator2D = new SquishAnimatorAdvanced2D(0.2f, 1, 1);

    private boolean updateTexture = false;
    public TextureRegion created;
    public TextureRegion createdReflection;

    private ClientEntity[] cachedNeighbours;

    private ClientPhysicsBody physicsBody;

    @Override
    public void onCreation() {
        disableTextureCentering = true;
        updateTextureBounds(16, 48, 0, 0);
        interactionPointArray = new float[] {
                clientPosX + 3, clientPosY + 3,
                clientPosX + 13, clientPosY + 3,
                clientPosX + 3, clientPosY + 13,
                clientPosX + 13, clientPosY + 13,
        };

        createTexture();

        checkForBoundingBox();
    }

    private void createTexture() {
        String elevationName;
        int ad0 = layerIds[0] - emulatingType.TILE_ID_DATA[0];
        String ev = emulatingType.name().toLowerCase();

        if(layerIds.length == 1) {
            if(ad0 == 0) {
                elevationName = "tile_" + ev + "_elevation_1";
            } else {
                elevationName = "tile_" + ev + "_elevation_2";
            }
        } else {
            int ad1 = layerIds[1] - emulatingType.TILE_ID_DATA[0];

            if(ad0 == 12 && ad1 == 17) {
                elevationName = "tile_" + ev + "_elevation_2";
            } else if(ad0 == 12) {
                elevationName = "tile_" + ev + "_elevation_4";
            } else if(ad1 == 17) {
                elevationName = "tile_" + ev + "_elevation_3";
            } else {
                elevationName = "tile_" + ev + "_elevation_1";
            }
        }

        // Check if the elevation texture has variations.
        int potentialVariations = ExpoAssets.get().getTileSheet().getAmountOfVariations(layerIds[0]);

        if(potentialVariations == 0) {
            potentialVariations = -1;
        } else {
            potentialVariations = MathUtils.random(0, potentialVariations - 1);
        }

        created = ExpoAssets.get().toTexture(layerIds, elevationName, potentialVariations);
        createdReflection = new TextureRegion(created, 0, 16, created.getRegionWidth(), created.getRegionHeight() - 16);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("stone_break");
        }

        if(physicsBody != null) {
            physicsBody.dispose();
            physicsBody = null;
        }
    }

    public void checkForBoundingBox() {
        if(physicsBody == null) {
            if(ServerDynamic3DTile.hasBoundingBox(layerIds, emulatingType)) {
                physicsBody = new ClientPhysicsBody(this, 0, 0, 16, 16);
            }
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("stone_hit");

        ClientEntity ce = entityManager().getEntityById(damageSourceEntityId);

        if(ce != null && ce.getEntityType() != ClientEntityType.THROWN_ENTITY) {
            squishAnimator2D.reset();
        }

        if(newHealth <= 0) {
            if(selected) {
                CameraShake.invoke(1.25f, 0.33f);
            }
            ParticleSheet.Common.spawnDustConstructFloorParticles(clientPosX, clientPosY);
        }

        ParticleSheet.Common.spawnDynamic3DHitParticles(this);
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(updateTexture) {
            updateTexture = false;
            createTexture();
        }

        if(visibleToRenderEngine) {
            cachedNeighbours = getNeighbouringTileEntitiesNESW();

            ClientPlayer local = ClientPlayer.getLocalPlayer();
            boolean playerBehind;

            if(local != null && (cachedNeighbours[0] == null || cachedNeighbours[0].getEntityType() != ClientEntityType.DYNAMIC_3D_TILE)) {
                if(local.depth > (depth + 15)) {
                    float buffer = 32;

                    playerBehind = ExpoShared.overlap(new float[] {
                            local.finalDrawPosX, local.finalDrawPosY,
                            local.finalDrawPosX + local.textureWidth, local.finalDrawPosY + local.textureHeight
                    }, new float[] {
                            finalTextureStartX - buffer, finalTextureStartY + 32,
                            finalTextureStartX + textureWidth + buffer, finalTextureStartY + 48
                    });
                } else {
                    playerBehind = false;
                }
            } else {
                playerBehind = false;
            }

            float MAX_BEHIND_DELTA = 0.4f;
            float MAX_BEHIND_STRENGTH = 0.5f;
            Interpolation useInterpolation = Interpolation.circleIn;

            if(playerBehind && playerBehindDelta < MAX_BEHIND_DELTA) {
                playerBehindDelta += delta;
                if(playerBehindDelta > MAX_BEHIND_DELTA) playerBehindDelta = MAX_BEHIND_DELTA;

                playerBehindDeltaInterpolated = useInterpolation.apply(playerBehindDelta / MAX_BEHIND_DELTA) * MAX_BEHIND_STRENGTH;
            }

            if(!playerBehind && playerBehindDelta > 0.0f) {
                playerBehindDelta -= delta;
                if(playerBehindDelta < 0) playerBehindDelta = 0;

                playerBehindDeltaInterpolated = useInterpolation.apply(playerBehindDelta / MAX_BEHIND_DELTA) * MAX_BEHIND_STRENGTH;
            }
        } else {
            cachedNeighbours = null;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            squishAnimator2D.calculate(delta);

            if(cachedNeighbours == null) cachedNeighbours = getNeighbouringTileEntitiesNESW();
            notifySquish();

            boolean hasNeighbourBelow = cachedNeighbours[2] != null && cachedNeighbours[2].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            float linePxFix = hasNeighbourBelow ? 0.1f : 0f;

            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            boolean naturalWall = emulatingType == TileLayerType.ROCK || emulatingType == TileLayerType.DIRT;
            boolean drawBody = layerIds.length > 1 || (layerIds[0] - emulatingType.TILE_ID_DATA[0] != 0);

            if(drawBody || !naturalWall) {
                rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1f - playerBehindDeltaInterpolated);

                rc.arraySpriteBatch.draw(created,
                        finalDrawPosX - squishAnimator2D.squishX1,
                        finalDrawPosY + squishAnimator2D.squishY1 - linePxFix,
                        created.getRegionWidth() + squishAnimator2D.squishX2,
                        created.getRegionHeight() + squishAnimator2D.squishY2 + linePxFix);

                if(naturalWall) {
                    if(layerIds.length > 1) {
                        int l0 = layerIds[0] - emulatingType.TILE_ID_DATA[0];
                        int l1 = layerIds[1] - emulatingType.TILE_ID_DATA[0];
                        int l2 = layerIds[2] - emulatingType.TILE_ID_DATA[0];
                        int l3 = layerIds[3] - emulatingType.TILE_ID_DATA[0];

                        float c1 = l0 == 8 ? BLACK : TRANS;
                        float c2 = l2 == 14 ? BLACK : TRANS;
                        float c3 = l3 == 11 ? BLACK : TRANS;
                        float c4 = l1 == 5 ? BLACK : TRANS;

                        rc.arraySpriteBatch.drawCustomCorners(rc.square,
                                finalDrawPosX - squishAnimator2D.squishX1,
                                finalDrawPosY + squishAnimator2D.squishY1 - linePxFix + 32,
                                rc.square.getRegionWidth() + squishAnimator2D.squishX2,
                                rc.square.getRegionHeight() + squishAnimator2D.squishY2 + linePxFix, c1, c2, c3, c4);
                    }
                }
            } else {
                if(naturalWall) {
                    rc.arraySpriteBatch.setColor(Color.BLACK);
                    rc.arraySpriteBatch.draw(rc.square, finalDrawPosX, finalDrawPosY + 32);
                }
            }

            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    private final static float TRANS = new Color(0, 0, 0, 0).toFloatBits();     //   0
    private final static float BLACK = new Color(0, 0, 0, 1f).toFloatBits();    // < 0

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(createdReflection,
                finalDrawPosX - squishAnimator2D.squishX1,
                finalDrawPosY + squishAnimator2D.squishY1,
                createdReflection.getRegionWidth() + squishAnimator2D.squishX2,
                (createdReflection.getRegionHeight() + squishAnimator2D.squishY2) * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        if(physicsBody != null) {
            Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX - squishAnimator2D.squishX1, finalTextureStartY + squishAnimator2D.squishY1);
            float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(created, shadow);
            boolean draw = rc.verticesInBounds(vertices);

            if(draw) {
                rc.arraySpriteBatch.drawGradient(created, textureWidth + squishAnimator2D.squishX2, textureHeight + squishAnimator2D.squishY2, shadow);
            }
        }

        /*
        if(emulatingType == TileLayerType.OAKPLANKWALL && visibleToRenderEngine) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(ao, finalDrawPosX - 2, finalDrawPosY - 2);
        }
        */
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.DYNAMIC_3D_TILE;
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        parsePayload(payload);
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        parsePayload(payload);
        checkForBoundingBox();
    }

    private void parsePayload(Object[] payload) {
        layerIds = (int[]) payload[0];
        emulatingType = TileLayerType.serialIdToType((int) payload[1]);
        updateTexture = true;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        squishAnimator2D.calculate(delta);
        notifySquish();

        setSelectionValuesNoOutline();

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1f - playerBehindDeltaInterpolated);
        rc.arraySpriteBatch.draw(created,
                finalDrawPosX - squishAnimator2D.squishX1,
                finalDrawPosY + squishAnimator2D.squishY1,
                created.getRegionWidth() + squishAnimator2D.squishX2,
                created.getRegionHeight() + squishAnimator2D.squishY2);

        if(emulatingType == TileLayerType.ROCK || emulatingType == TileLayerType.DIRT) {
            if(layerIds.length > 1) {
                int l0 = layerIds[0] - emulatingType.TILE_ID_DATA[0];
                int l1 = layerIds[1] - emulatingType.TILE_ID_DATA[0];
                int l2 = layerIds[2] - emulatingType.TILE_ID_DATA[0];
                int l3 = layerIds[3] - emulatingType.TILE_ID_DATA[0];

                float c1 = l0 == 8 ? BLACK : TRANS;
                float c2 = l2 == 14 ? BLACK : TRANS;
                float c3 = l3 == 11 ? BLACK : TRANS;
                float c4 = l1 == 5 ? BLACK : TRANS;

                rc.arraySpriteBatch.drawCustomCorners(rc.square,
                        finalDrawPosX - squishAnimator2D.squishX1,
                        finalDrawPosY + squishAnimator2D.squishY1 + 32,
                        rc.square.getRegionWidth() + squishAnimator2D.squishX2,
                        rc.square.getRegionHeight() + squishAnimator2D.squishY2, c1, c2, c3, c4);
            }
        }

        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        rc.defaultArrayBatch();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    private void notifySquish() {
        // Notify neighbouring tiles.
        cachedNeighbours = getNeighbouringTileEntitiesNESW();
        boolean active = squishAnimator2D.isActive();

        if(active) {
            if(cachedNeighbours[0] != null && cachedNeighbours[0] instanceof ClientDynamic3DTile cd3d) {
                cd3d.squishAnimator2D.squishY1 = -squishAnimator2D.squishY1;
                cd3d.squishAnimator2D.squishY2 = -squishAnimator2D.squishY2 * 0.5f;
            }
            if(cachedNeighbours[1] != null && cachedNeighbours[1] instanceof ClientDynamic3DTile cd3d) {
                cd3d.squishAnimator2D.squishX1 = -squishAnimator2D.squishX1;
                cd3d.squishAnimator2D.squishX2 = -squishAnimator2D.squishX1;
            }
            if(cachedNeighbours[2] != null && cachedNeighbours[2] instanceof ClientDynamic3DTile cd3d) {
                cd3d.squishAnimator2D.squishY2 = -squishAnimator2D.squishY1 * 0.5f;
            }
            if(cachedNeighbours[3] != null && cachedNeighbours[3] instanceof ClientDynamic3DTile cd3d) {
                cd3d.squishAnimator2D.squishX2 = -squishAnimator2D.squishX1;
            }
        } else {
            if(squishAnimator2D.isFinished()) {
                if(cachedNeighbours[0] != null && cachedNeighbours[0] instanceof ClientDynamic3DTile cd3d) {
                    cd3d.squishAnimator2D.squishY1 = 0;
                    cd3d.squishAnimator2D.squishY2 = 0;
                }
                if(cachedNeighbours[1] != null && cachedNeighbours[1] instanceof ClientDynamic3DTile cd3d) {
                    cd3d.squishAnimator2D.squishX1 = 0;
                    cd3d.squishAnimator2D.squishX2 = 0;
                }
                if(cachedNeighbours[2] != null && cachedNeighbours[2] instanceof ClientDynamic3DTile cd3d) {
                    cd3d.squishAnimator2D.squishY2 = 0;
                }
                if(cachedNeighbours[3] != null && cachedNeighbours[3] instanceof ClientDynamic3DTile cd3d) {
                    cd3d.squishAnimator2D.squishX2 = 0;
                }
            }
        }
    }

}
