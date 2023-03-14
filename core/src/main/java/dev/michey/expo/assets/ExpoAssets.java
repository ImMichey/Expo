package dev.michey.expo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.render.ui.InteractableItemSlot;

import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoAssets {

    private final HashMap<String, TileMapping> tileMappings;
    private final AssetManager assetManager;
    private TextureAtlas mainAtlas;
    public TextureRegion soil;

    private TileSheet tileSheet;

    public ExpoAssets() {
        assetManager = new AssetManager();
        tileMappings = new HashMap<>();
    }

    public void loadAssets() {
        FileHandle fh = Gdx.files.internal("resourceLoader.txt");
        String[] lines = fh.readString().split(System.lineSeparator());

        Class<?> currentMode = null;

        for(String line : lines) {
            if(line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue; // ignore

            if(line.startsWith("[") && line.endsWith("]")) {
                String type = line.substring(1, line.length() - 1);

                switch (type) {
                    case "TextureAtlas" -> currentMode = TextureAtlas.class;
                    case "Texture" -> currentMode = Texture.class;
                    case "TileMapping" -> currentMode = TileMapping.class;
                    case "!LOAD" -> {
                        assetManager.finishLoading();

                        if(mainAtlas == null) {
                            mainAtlas = assetManager.get("textures/atlas/expo-0.atlas", TextureAtlas.class);
                            soil = textureRegion("soiltile");
                        }
                    }
                }
            } else {
                if(currentMode == null) {
                    log("Error while reading resourceLoader.txt: " + line + " cannot be read (currentMode = null)");
                    continue;
                }

                if(currentMode == TileMapping.class) {
                    TileMapping mapping = new TileMapping();

                    if(mapping.load(line)) {
                        tileMappings.put(line, mapping);
                    }
                } else {
                    assetManager.load("textures/" + line, currentMode);
                }
            }
        }

        tileSheet = new TileSheet(textureRegion("tileset"));
    }

    public TileSheet getTileSheet() {
        return tileSheet;
    }

    public void slice() {
        TextureRegion raw = textureRegion("sandtiles");
        Texture tex = raw.getTexture();
        if(!tex.getTextureData().isPrepared()) {
            tex.getTextureData().prepare();
        }
        Pixmap pixmap = tex.getTextureData().consumePixmap();

        int perTile = 16;
        int w = raw.getRegionWidth();
        int h = raw.getRegionHeight();

        int xx = w / perTile;
        int yy = h / perTile;

        for(int x = 0; x < xx; x++) {
            for(int y = 0; y < yy; y++) {
                int index = y * xx + x;
                //log("Index X" + x + " Y" + y + " -> " + index);
                TextureRegion part = new TextureRegion(raw, x * perTile, y * perTile, perTile, perTile);
                Pixmap localPixmap = new Pixmap(
                        16,
                        16,
                        pixmap.getFormat()
                );

                localPixmap.drawPixmap(pixmap, 0, 0, part.getRegionX(), part.getRegionY(), 16, 16);

                Gdx.files.external("TEST").mkdirs();
                FileHandle fh = Gdx.files.external("TEST/tile_sand_" + index + ".png");

                PixmapIO.writePNG(fh, localPixmap);
            }
        }
    }

    public TileMapping getTileMapping(String key) {
        return tileMappings.get(key);
    }

    public TileMapping biomeToTileMapping(BiomeType type) {
        return switch (type) {
            case GRASS -> getTileMapping("atlas/tile_mappings_grass");
            case BEACH -> getTileMapping("atlas/tile_mappings_sand");
            default -> null;
        };
    }

    public TextureRegion textureRegion(String name) {
        return mainAtlas.findRegion(name);
    }

    public TextureRegion textureRegionFresh(String name) {
        return new TextureRegion(textureRegion(name));
    }

    public Texture texture(String name) {
        return assetManager.get("textures/" + name, Texture.class);
    }

    /** Singleton */
    private static ExpoAssets INSTANCE;

    public static ExpoAssets get() {
        if(INSTANCE == null) {
            INSTANCE = new ExpoAssets();
        }

        return INSTANCE;
    }

}
