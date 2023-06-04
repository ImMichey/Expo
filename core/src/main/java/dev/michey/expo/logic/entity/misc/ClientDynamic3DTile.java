package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientDynamic3DTile extends ClientEntity implements SelectableEntity {

    public TileLayerType emulatingType;
    public int[] layerIds;
    private float[] interactionPointArray;

    private float playerBehindDelta = 1.0f;

    private boolean updateTexture = false;
    private TextureRegion created;

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
    }

    private void createTexture() {
        String elevationName;

        if(layerIds.length == 1) {
            elevationName = "tile_rock_elevation_1";
        } else {
            int ad0 = layerIds[0] - emulatingType.TILE_ID_DATA[0];
            int ad1 = layerIds[1] - emulatingType.TILE_ID_DATA[0];

            if(ad0 == 12 && ad1 == 17) {
                elevationName = "tile_rock_elevation_2";
            } else if(ad0 == 12) {
                elevationName = "tile_rock_elevation_4";
            } else if(ad1 == 17) {
                elevationName = "tile_rock_elevation_3";
            } else {
                elevationName = "tile_rock_elevation_1";
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
    public void onDamage(float damage, float newHealth) {
        playEntitySound("stone_hit");
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();

        if(updateTexture) {
            updateTexture = false;
            createTexture();
        }

        ClientPlayer local = ClientPlayer.getLocalPlayer();
        boolean playerBehind;

        if(local != null) {
            if(local.depth > depth) {
                playerBehind = RenderContext.get().entityVerticesIntersecting(new float[] {
                        local.clientPosX, local.clientPosY,
                        local.clientPosX + local.textureWidth, local.clientPosY + local.textureHeight
                }, new float[] {
                        finalTextureStartX, finalTextureStartY,
                        finalTextureStartX + textureWidth, finalTextureStartY + textureHeight
                });
            } else {
                playerBehind = false;
            }
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
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, playerBehindDelta);
            rc.arraySpriteBatch.draw(created, finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
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
        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, Color.WHITE, true);

        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, playerBehindDelta);
        rc.arraySpriteBatch.draw(created, finalDrawPosX, finalDrawPosY);

        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

}
