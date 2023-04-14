package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class TileSheet {

    private final ExpoAssets assets;

    public final TextureRegion baseSoil;
    public final TextureRegion notSet;

    private final HashMap<Integer, TextureRegion> tilesetTextureMap;
    private final HashMap<Integer, TextureRegion[]> tilesetVariationTextureMap;
    private int currentId;

    public TileSheet(ExpoAssets assets) {
        this.assets = assets;
        tilesetTextureMap = new HashMap<>();
        tilesetVariationTextureMap = new HashMap<>();

        baseSoil = singleEntry("tile_soil");
        multiEntry("tile_grass", 22);
        multiEntry("tile_sand", 22);
        notSet = singleEntry("tile_not_set");
        multiEntry("tile_ocean", 22);
        multiEntry("tile_ocean_deep", 22);
        multiEntry("tile_soil_hole", 22);

        // Variations
        variationEntry("tile_grass", 1, 7);
    }

    private void variationEntry(String name, int forId, int amount) {
        TextureRegion[] array = new TextureRegion[amount];
        for(int i = 0; i < amount; i++) array[i] = assets.textureRegion(name + "_variation_" + i);
        tilesetVariationTextureMap.put(forId, array);
    }

    private TextureRegion singleEntry(String name) {
        TextureRegion tex = assets.textureRegion(name);
        tilesetTextureMap.put(currentId, tex);
        currentId++;
        return tex;
    }

    private void multiEntry(String name, int amount) {
        log("TileSheet entry: " + name + " " + currentId + " - " + (currentId + amount - 1));
        for(int i = 0; i < amount; i++) tilesetTextureMap.put(currentId + i, assets.textureRegion(name + "_" + i));
        currentId += amount;
    }

    public HashMap<Integer, TextureRegion> getTilesetTextureMap() {
        return tilesetTextureMap;
    }

    public TextureRegion getRandomVariation(int id) {
        TextureRegion[] array = tilesetVariationTextureMap.get(id);
        return array[MathUtils.random(0, array.length - 1)];
    }

    public boolean isFullTile(int tileId) {
        return switch (tileId) {
            case 1, 23 -> true;
            default -> false;
        };
    }

}
