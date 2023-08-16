package dev.michey.expo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.log.ExpoLogger;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoAssets {

    private final AssetManager assetManager;
    private TextureAtlas expo0Atlas;
    private TextureAtlas tilesAtlas;

    private TileSheet tileSheet;
    private ParticleSheet particleSheet;
    private ItemSheet itemSheet;

    private final StringBuilder tileNameBuilder;

    public ExpoAssets() {
        assetManager = new AssetManager();
        tileNameBuilder = new StringBuilder();
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
                    case "Texture" -> currentMode = Texture.class;
                    case "!LOAD" -> assetManager.finishLoading();
                }
            } else {
                if(currentMode == null) {
                    log("Error while reading resourceLoader.txt: " + line + " cannot be read (currentMode = null)");
                    continue;
                }

                if(!Gdx.files.internal("textures/" + line).exists()) {
                    ExpoLogger.log("ResourceLoader WARNING: The file textures/" + line + " does not exist, skipping...");
                } else {
                    assetManager.load("textures/" + line, currentMode);
                }
            }
        }

        assetManager.load("textures/atlas/expo-0.atlas", TextureAtlas.class);
        assetManager.load("textures/atlas/tiles.atlas", TextureAtlas.class);
        assetManager.finishLoading();
        expo0Atlas = assetManager.get("textures/atlas/expo-0.atlas", TextureAtlas.class);
        tilesAtlas = assetManager.get("textures/atlas/tiles.atlas", TextureAtlas.class);

        tileSheet = new TileSheet(this);
        particleSheet = new ParticleSheet(textureRegion("particlesheet"));
        itemSheet = new ItemSheet(textureRegion("itemsheet"));
    }

    public TileSheet getTileSheet() {
        return tileSheet;
    }

    public ParticleSheet getParticleSheet() {
        return particleSheet;
    }

    public ItemSheet getItemSheet() {
        return itemSheet;
    }

    public void slice(String tileName, boolean singleTile, int startX, int startY, int yTileHeight) {
        TextureRegion tiles = textureRegion("tileset");
        Texture tex = tiles.getTexture();
        if(!tex.getTextureData().isPrepared()) tex.getTextureData().prepare();
        Pixmap pixmap = tex.getTextureData().consumePixmap();
        Gdx.files.external("convertedTiles").mkdirs();

        if(singleTile) {
            TextureRegion tileSingle = new TextureRegion(tiles, startX, startY, 16, yTileHeight);
            Pixmap localPixmap = new Pixmap(16, yTileHeight, pixmap.getFormat());
            localPixmap.drawPixmap(pixmap, 0, 0, tileSingle.getRegionX(), tileSingle.getRegionY(), 16, yTileHeight);
            FileHandle fh = Gdx.files.external("convertedTiles/" + tileName + ".png");
            PixmapIO.writePNG(fh, localPixmap);
            localPixmap.dispose();
        } else {
            TextureRegion[] slices = new TextureRegion[22];
            int cutoff = yTileHeight == 48 ? 16 : 8;
            int remainder = yTileHeight - cutoff;

            // Full all sides texture
            slices[0] = new TextureRegion(tiles, startX, startY, 16, yTileHeight);

            // Single texture
            slices[1] = new TextureRegion(tiles, startX + 16, startY, 16, yTileHeight);

            // Top Left Corner 2x2
            slices[2] = new TextureRegion(tiles, startX + 32, startY, 8, cutoff);
            slices[3] = new TextureRegion(tiles, startX + 40, startY, 8, cutoff);
            slices[4] = new TextureRegion(tiles, startX + 32, startY + cutoff, 8, remainder);
            slices[5] = new TextureRegion(tiles, startX + 40, startY + cutoff, 8, remainder);

            // Top Right Corner 2x2
            slices[6] = new TextureRegion(tiles, startX + 48, startY, 8, cutoff);
            slices[7] = new TextureRegion(tiles, startX + 56, startY, 8, cutoff);
            slices[8] = new TextureRegion(tiles, startX + 48, startY + cutoff, 8, remainder);
            slices[9] = new TextureRegion(tiles, startX + 56, startY + cutoff, 8, remainder);

            // Bottom Left Corner 2x2
            slices[10] = new TextureRegion(tiles, startX + 32, startY + yTileHeight, 8, cutoff);
            slices[11] = new TextureRegion(tiles, startX + 40, startY + yTileHeight, 8, cutoff);
            slices[12] = new TextureRegion(tiles, startX + 32, startY + yTileHeight + cutoff, 8, remainder);
            slices[13] = new TextureRegion(tiles, startX + 40, startY + yTileHeight + cutoff, 8, remainder);

            // Bottom Right Corner 2x2
            slices[14] = new TextureRegion(tiles, startX + 48, startY + yTileHeight, 8, cutoff);
            slices[15] = new TextureRegion(tiles, startX + 56, startY + yTileHeight, 8, cutoff);
            slices[16] = new TextureRegion(tiles, startX + 48, startY + yTileHeight + cutoff, 8, remainder);
            slices[17] = new TextureRegion(tiles, startX + 56, startY + yTileHeight + cutoff, 8, remainder);

            // Transition corners 2x2
            slices[18] = new TextureRegion(tiles, startX + 16, startY + yTileHeight, 8, cutoff);
            slices[19] = new TextureRegion(tiles, startX + 24, startY + yTileHeight, 8, cutoff);
            slices[20] = new TextureRegion(tiles, startX + 16, startY + yTileHeight + cutoff, 8, remainder);
            slices[21] = new TextureRegion(tiles, startX + 24, startY + yTileHeight + cutoff, 8, remainder);

            for(int i = 0; i < slices.length; i++) {
                TextureRegion toConsume = slices[i];
                Pixmap localPixmap = new Pixmap(toConsume.getRegionWidth(), toConsume.getRegionHeight(), pixmap.getFormat());
                localPixmap.drawPixmap(pixmap, 0, 0, toConsume.getRegionX(), toConsume.getRegionY(), toConsume.getRegionWidth(), toConsume.getRegionHeight());
                FileHandle fh = Gdx.files.external("convertedTiles/" + tileName + "_" + i + ".png");
                PixmapIO.writePNG(fh, localPixmap);
                localPixmap.dispose();
            }
        }
    }

    public void slice(String tileName, boolean singleTile, int startX, int startY) {
        slice(tileName, singleTile, startX, startY, 16);
    }

    public TextureRegion findTile(String name) {
        return tilesAtlas.findRegion(name);
    }

    public TextureRegion toTexture(int[] ids, String elevationName, int variation) {
        tileNameBuilder.setLength(0);
        tileNameBuilder.append('t');

        for(int i = 0; i < ids.length; i++) {
            tileNameBuilder.append(ids[i]);

            if(i < (ids.length - 1)) {
                tileNameBuilder.append(',');
            }
        }

        if(elevationName != null) {
            tileNameBuilder.append(',');
            tileNameBuilder.append(elevationName);
        }

        if(variation >= 0) {
            tileNameBuilder.append(',');
            tileNameBuilder.append("variation_");
            tileNameBuilder.append(variation);
        }

        return tilesAtlas.findRegion(tileNameBuilder.toString());
    }

    public TextureRegion textureRegion(String name) {
        return expo0Atlas.findRegion(name);
    }

    public TextureRegion textureRegionFresh(String name) {
        return new TextureRegion(textureRegion(name));
    }

    public Texture texture(String name) {
        return assetManager.get("textures/" + name, Texture.class);
    }

    public Array<TextureRegion> textureArray(String name, int frames) {
        Array<TextureRegion> array = new Array<>(frames);
        for(int i = 0; i < frames; i++) {
            array.add(textureRegionFresh(name + "_" + (i + 1)));
        }
        return array;
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
