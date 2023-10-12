package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.SquishAnimator2D;
import dev.michey.expo.render.animator.SquishAnimatorAdvanced2D;
import dev.michey.expo.render.camera.CameraShake;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;

import java.util.Arrays;

public class ClientDynamic3DTile extends ClientEntity implements SelectableEntity, ReflectableEntity {

    public TileLayerType emulatingType;
    public int[] layerIds;
    private float[] interactionPointArray;

    private float playerBehindDelta;
    private float playerBehindDeltaInterpolated;

    public final SquishAnimatorAdvanced2D squishAnimator2D = new SquishAnimatorAdvanced2D(0.2f, 1, 1);

    private boolean updateTexture = false;
    public TextureRegion created;
    private TextureRegion ao;

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

        ao = tr("entity_wall_ao");
        createTexture();
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

        created = ExpoAssets.get().toTexture(layerIds, elevationName, -1);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("stone_break");
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("stone_hit");
        squishAnimator2D.reset();

        if(selected && newHealth <= 0) {
            CameraShake.invoke(1.25f, 0.33f);
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(updateTexture) {
            updateTexture = false;
            createTexture();
        }

        /*
        ClientPlayer local = ClientPlayer.getLocalPlayer();
        boolean playerBehind;

        if(local != null) {
            if(local.depth > depth) {
                float buffer = 4;

                playerBehind = ExpoShared.overlap(new float[] {
                        local.clientPosX, local.clientPosY,
                        local.clientPosX + local.textureWidth, local.clientPosY + local.textureHeight
                }, new float[] {
                        finalTextureStartX - buffer, finalTextureStartY - buffer,
                        finalTextureStartX + textureWidth + buffer, finalTextureStartY + textureHeight + buffer
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
        */
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            boolean un = squishAnimator2D.isActive();
            ClientEntity[] n = getNeighbouringTileEntitiesNESW();
            squishAnimator2D.calculate(delta);
            if(un) notifySquish(n);

            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1f - playerBehindDeltaInterpolated);
            rc.arraySpriteBatch.draw(created,
                    finalDrawPosX - squishAnimator2D.squishX1,
                    finalDrawPosY + squishAnimator2D.squishY1,
                    created.getRegionWidth() + squishAnimator2D.squishX2,
                    created.getRegionHeight() + squishAnimator2D.squishY2);
            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(created,
                finalDrawPosX - squishAnimator2D.squishX1,
                finalDrawPosY + squishAnimator2D.squishY1,
                created.getRegionWidth() + squishAnimator2D.squishX2,
                (created.getRegionHeight() + squishAnimator2D.squishY2) * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        if(ServerDynamic3DTile.hasBoundingBox(layerIds, emulatingType)) {
            Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
            float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(created, shadow);
            boolean draw = rc.verticesInBounds(vertices);

            if(draw) {
                rc.useArrayBatch();
                rc.useRegularArrayShader();
                rc.arraySpriteBatch.drawGradient(created, textureWidth, textureHeight, shadow);
            }
        }

        if(emulatingType == TileLayerType.OAKPLANKWALL) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(ao, finalDrawPosX - 2, finalDrawPosY - 2);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.DYNAMIC_3D_TILE;
    }

    @Override
    public void applyPacketPayload(Object[] payload) {
        parsePayload(payload);
    }

    @Override
    public void readEntityDataUpdate(Object[] payload) {
        parsePayload(payload);
    }

    private void parsePayload(Object[] payload) {
        layerIds = (int[]) payload[0];
        emulatingType = TileLayerType.serialIdToType((int) payload[1]);
        updateTexture = true;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        boolean un = squishAnimator2D.isActive();
        ClientEntity[] n = getNeighbouringTileEntitiesNESW();
        squishAnimator2D.calculate(delta);
        if(un) notifySquish(n);

        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, Color.WHITE, true);

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1f - playerBehindDeltaInterpolated * 0.5f);
        rc.arraySpriteBatch.draw(created,
                finalDrawPosX - squishAnimator2D.squishX1,
                finalDrawPosY + squishAnimator2D.squishY1,
                created.getRegionWidth() + squishAnimator2D.squishX2,
                created.getRegionHeight() + squishAnimator2D.squishY2);

        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    private void notifySquish(ClientEntity[] n) {
        // Notify neighbouring tiles.
        if(n[0] != null && n[0] instanceof ClientDynamic3DTile cd3d) {
            cd3d.squishAnimator2D.squishY1 = -squishAnimator2D.squishY1;
            cd3d.squishAnimator2D.squishY2 = -squishAnimator2D.squishY2 * 0.5f;
        }
        if(n[1] != null && n[1] instanceof ClientDynamic3DTile cd3d) {
            cd3d.squishAnimator2D.squishX1 = -squishAnimator2D.squishX1;
            cd3d.squishAnimator2D.squishX2 = -squishAnimator2D.squishX1;
        }
        if(n[2] != null && n[2] instanceof ClientDynamic3DTile cd3d) {
            cd3d.squishAnimator2D.squishY2 = -squishAnimator2D.squishY1 * 0.5f;
        }
        if(n[3] != null && n[3] instanceof ClientDynamic3DTile cd3d) {
            cd3d.squishAnimator2D.squishX2 = -squishAnimator2D.squishX1;
        }
    }

}
