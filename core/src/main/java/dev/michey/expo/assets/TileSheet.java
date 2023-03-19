package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.Expo;

import java.util.Arrays;
import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class TileSheet {

    private final ExpoAssets assets;

    public final TextureRegion baseSoil;
    public final TextureRegion notSet;

    private final HashMap<Integer, TextureRegion> tilesetTextureMap;
    private int currentId;

    public TileSheet(ExpoAssets assets) {
        this.assets = assets;
        tilesetTextureMap = new HashMap<>();

        baseSoil = singleEntry("tile_soil");
        multiEntry("tile_grass", 22);
        multiEntry("tile_sand", 22);
        notSet = singleEntry("tile_not_set");
        multiEntry("tile_ocean", 22);
        multiEntry("tile_ocean_deep", 22);
    }

    private TextureRegion singleEntry(String name) {
        TextureRegion tex = assets.textureRegion(name);
        tilesetTextureMap.put(currentId, tex);
        currentId++;
        return tex;
    }

    private void multiEntry(String name, int amount) {
        for(int i = 0; i < amount; i++) tilesetTextureMap.put(currentId + i, assets.textureRegion(name + "_" + i));
        currentId += amount;
    }

    public HashMap<Integer, TextureRegion> getTilesetTextureMap() {
        return tilesetTextureMap;
    }

}
