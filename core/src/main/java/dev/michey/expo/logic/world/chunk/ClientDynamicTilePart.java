package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;

public class ClientDynamicTilePart {

    /** The chunk this tile belongs to. */
    private final ClientChunk chunk;
    private final float x, y;

    public TileLayerType emulatingType;
    public int[] layerIds;
    public TextureRegion[] texture;

    public ClientDynamicTilePart(ClientChunk chunk, float x, float y, DynamicTilePart serverTile) {
        this.chunk = chunk;
        this.x = x;
        this.y = y;
        updateFrom(serverTile);
    }

    public void updateFrom(DynamicTilePart serverTile) {
        this.emulatingType = serverTile.emulatingType;
        this.layerIds = serverTile.layerIds;
        generateTextures();
    }

    public void generateTextures() {
        int size = layerIds.length;
        if((emulatingType != TileLayerType.WATER && emulatingType != TileLayerType.WATER_SANDY) && size == 4) {
            size = 1;
        }
        texture = new TextureRegion[size];

        if(emulatingType == TileLayerType.WATER || emulatingType == TileLayerType.WATER_SANDY) {
            for(int i = 0; i < texture.length; i++) {
                texture[i] = ExpoAssets.get().getTileSheet().getTilesetTextureMap().get(layerIds[i]);
            }
        } else {
            int potentialVariations = ExpoAssets.get().getTileSheet().getAmountOfVariations(layerIds[0]);

            if(potentialVariations > 0 && MathUtils.random() <= 0.75f) {
                texture[0] = ExpoAssets.get().toTexture(layerIds, null, MathUtils.random(0, potentialVariations - 1));
            } else {
                texture[0] = ExpoAssets.get().toTexture(layerIds, null, -1);
            }
        }
    }

    public boolean isFullTile() {
        return switch (layerIds[0]) {
            case 1, 23, 112, 133, 156, 288 -> true;
            default -> false;
        };
    }

    public void drawSimple() {
        RenderContext r = RenderContext.get();

        if(texture.length == 1) {
            if(texture[0] == null) return;
            r.batch.draw(texture[0], x, y, 16, 16);
        } else {
            r.batch.draw(texture[0], x, y, 8, 8);
            r.batch.draw(texture[1], x + 8, y, 8, 8);
            r.batch.draw(texture[2], x , y + 8, 8, 8);
            r.batch.draw(texture[3], x + 8, y + 8, 8, 8);
        }
    }

    public void draw(RenderContext r, ClientChunk chunk, boolean ignore, int chunkIndex) {
        if(texture.length == 1) {
            if(texture[0] == null) return;
            r.polygonTileBatch.drawGrass(texture[0], x, y, 16, 16, chunk, ignore, chunkIndex);
        } else {
            r.polygonTileBatch.drawGrass(texture[0], x, y, 8, 8, chunk, ignore, chunkIndex);
            r.polygonTileBatch.drawGrass(texture[1], x + 8, y, 8, 8, chunk, ignore, chunkIndex);
            r.polygonTileBatch.drawGrass(texture[2], x , y + 8, 8, 8, chunk, ignore, chunkIndex);
            r.polygonTileBatch.drawGrass(texture[3], x + 8, y + 8, 8, 8, chunk, ignore, chunkIndex);
        }
    }

}