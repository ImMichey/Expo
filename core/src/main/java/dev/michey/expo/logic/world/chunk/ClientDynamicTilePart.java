package dev.michey.expo.logic.world.chunk;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;

import java.util.Arrays;

public class ClientDynamicTilePart {

    public TileLayerType emulatingType;
    public int[] layerIds;
    public TextureRegion[] textures;

    public ClientDynamicTilePart(DynamicTilePart serverTile) {
        updateFrom(serverTile, true);
    }

    public void updateFrom(DynamicTilePart serverTile, boolean ignoreTextureCheck) {
        this.emulatingType = serverTile.emulatingType;

        if(!ignoreTextureCheck) {
            int[] oldLayerIds = layerIds;
            this.layerIds = serverTile.layerIds;

            if(!Arrays.equals(oldLayerIds, layerIds)) {
                generateTextures();
            }
        } else {
            this.layerIds = serverTile.layerIds;
            generateTextures();
        }
    }

    public void generateTextures() {
        textures = new TextureRegion[layerIds.length];

        for(int i = 0; i < layerIds.length; i++) {
            TextureRegion generated;
            int id = layerIds[i];

            if(ExpoAssets.get().getTileSheet().hasVariation(id)) {
                generated = ExpoAssets.get().getTileSheet().getRandomVariation(id);
            } else {
                generated = ExpoAssets.get().getTileSheet().getTilesetTextureMap().get(id);
            }

            textures[i] = generated;
        }
    }

}
