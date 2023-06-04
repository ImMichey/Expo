package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.arraybatch.TileMerger;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.util.ClientUtils;
import dev.michey.expo.util.Pair;;

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
        if(emulatingType != TileLayerType.WATER && size == 4) {
            size = 1;
        }
        texture = new TextureRegion[size];

        if(emulatingType == TileLayerType.WATER) {
            for(int i = 0; i < texture.length; i++) {
                texture[i] = ExpoAssets.get().getTileSheet().getTilesetTextureMap().get(layerIds[i]);
            }
        } else {
            int potentialVariations = ExpoAssets.get().getTileSheet().getAmountOfVariations(layerIds[0]);

            if(potentialVariations > 0) {
                texture[0] = TileMerger.get().getCombinedTile(layerIds, null, MathUtils.random(0, potentialVariations - 1), false);
            } else {
                texture[0] = TileMerger.get().getCombinedTile(layerIds, null, -1, false);
            }
        }
    }

    public void draw(RenderContext r, Pair[] displacementPairs) {
        ClientUtils.log("TEST: " + texture.length + " @  " + texture[0] + " " + (texture[0] == null ? "null" : texture[0].getTexture()), Input.Keys.I);
        if(texture.length == 1) {
            if(texture[0] == null) return;
            r.batch.draw(texture[0], x, y);
        } else {
            float val = ExpoClientContainer.get().getClientWorld().getClientChunkGrid().interpolation;

            r.batch.draw(texture[0], x + val * (int) displacementPairs[0].key, y + val * (int) displacementPairs[0].value);
            r.batch.draw(texture[1], x + 8 + val * (int) displacementPairs[1].key, y + val * (int) displacementPairs[1].value);
            r.batch.draw(texture[2], x + val * (int) displacementPairs[2].key, y + 8 + val * (int) displacementPairs[2].value);
            r.batch.draw(texture[3], x + 8 + val * (int) displacementPairs[3].key, y + 8 + val * (int) displacementPairs[3].value);
        }
    }

}
