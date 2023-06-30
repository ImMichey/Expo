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
        multiEntry("tile_forest", 22);
        multiEntry("tile_desert", 22);
        multiEntry("tile_rock", 22);
        multiEntry("tile_soil_farmland", 22);

        // Variations
        variationEntry("tile_grass", 1, 1);
        variationEntry("tile_forest", 112, 1);
    }

    private void variationEntry(String name, int forId, int amount) {
        TextureRegion[] array = new TextureRegion[amount];
        for(int i = 0; i < amount; i++) array[i] = assets.findTile(name + "_variation_" + i);
        tilesetVariationTextureMap.put(forId, array);
    }

    private TextureRegion singleEntry(String name) {
        TextureRegion tex = assets.findTile(name);
        tilesetTextureMap.put(currentId, tex);
        currentId++;
        return tex;
    }

    private void multiEntry(String name, int amount) {
        log("TileSheet entry: " + name + " " + currentId + " - " + (currentId + amount - 1));
        for(int i = 0; i < amount; i++) tilesetTextureMap.put(currentId + i, assets.findTile(name + "_" + i));
        currentId += amount;
    }

    public TextureRegion getVariation(int id, int variation) {
        return tilesetVariationTextureMap.get(id)[variation];
    }

    public HashMap<Integer, TextureRegion> getTilesetTextureMap() {
        return tilesetTextureMap;
    }

    public TextureRegion getRandomVariation(int id) {
        TextureRegion[] array = tilesetVariationTextureMap.get(id);
        return array[MathUtils.random(0, array.length - 1)];
    }

    public int getAmountOfVariations(int id) {
        TextureRegion[] variations = tilesetVariationTextureMap.get(id);
        if(variations == null) return 0;
        return variations.length;
    }

    public boolean hasVariation(int id) {
        return tilesetVariationTextureMap.containsKey(id);
    }

    public boolean isFullTile(int tileId) {
        return switch (tileId) {
            case 1, 23, 112, 133, 156 -> true;
            default -> false;
        };
    }

}
