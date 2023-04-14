package dev.michey.expo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoAssets {

    private final AssetManager assetManager;
    private TextureAtlas mainAtlas;

    private TileSheet tileSheet;
    private ParticleSheet particleSheet;
    private ItemSheet itemSheet;

    public ExpoAssets() {
        assetManager = new AssetManager();
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
                    case "!LOAD" -> {
                        assetManager.finishLoading();

                        if(mainAtlas == null) {
                            mainAtlas = assetManager.get("textures/atlas/expo-0.atlas", TextureAtlas.class);
                        }
                    }
                }
            } else {
                if(currentMode == null) {
                    log("Error while reading resourceLoader.txt: " + line + " cannot be read (currentMode = null)");
                    continue;
                }

                assetManager.load("textures/" + line, currentMode);
            }
        }

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

    public void slice(String tileName, boolean singleTile, int startX, int startY) {
        TextureRegion tiles = textureRegion("tileset");
        Texture tex = tiles.getTexture();
        if(!tex.getTextureData().isPrepared()) tex.getTextureData().prepare();
        Pixmap pixmap = tex.getTextureData().consumePixmap();
        Gdx.files.external("convertedTiles").mkdirs();

        if(singleTile) {
            TextureRegion tileSingle = new TextureRegion(tiles, startX, startY, 16, 16);
            Pixmap localPixmap = new Pixmap(16, 16, pixmap.getFormat());
            localPixmap.drawPixmap(pixmap, 0, 0, tileSingle.getRegionX(), tileSingle.getRegionY(), 16, 16);
            FileHandle fh = Gdx.files.external("convertedTiles/" + tileName + ".png");
            PixmapIO.writePNG(fh, localPixmap);
            localPixmap.dispose();
        } else {
            TextureRegion[] slices = new TextureRegion[22];

            // Full all sides texture
            slices[0] = new TextureRegion(tiles, startX, startY, 16, 16);

            // Single texture
            slices[1] = new TextureRegion(tiles, startX + 16, startY, 16, 16);

            // Top Left Corner 2x2
            slices[2] = new TextureRegion(tiles, startX + 32, startY, 8, 8);
            slices[3] = new TextureRegion(tiles, startX + 40, startY, 8, 8);
            slices[4] = new TextureRegion(tiles, startX + 32, startY + 8, 8, 8);
            slices[5] = new TextureRegion(tiles, startX + 40, startY + 8, 8, 8);

            // Top Right Corner 2x2
            slices[6] = new TextureRegion(tiles, startX + 48, startY, 8, 8);
            slices[7] = new TextureRegion(tiles, startX + 56, startY, 8, 8);
            slices[8] = new TextureRegion(tiles, startX + 48, startY + 8, 8, 8);
            slices[9] = new TextureRegion(tiles, startX + 56, startY + 8, 8, 8);

            // Bottom Left Corner 2x2
            slices[10] = new TextureRegion(tiles, startX + 32, startY + 16, 8, 8);
            slices[11] = new TextureRegion(tiles, startX + 40, startY + 16, 8, 8);
            slices[12] = new TextureRegion(tiles, startX + 32, startY + 24, 8, 8);
            slices[13] = new TextureRegion(tiles, startX + 40, startY + 24, 8, 8);

            // Bottom Right Corner 2x2
            slices[14] = new TextureRegion(tiles, startX + 48, startY + 16, 8, 8);
            slices[15] = new TextureRegion(tiles, startX + 56, startY + 16, 8, 8);
            slices[16] = new TextureRegion(tiles, startX + 48, startY + 24, 8, 8);
            slices[17] = new TextureRegion(tiles, startX + 56, startY + 24, 8, 8);

            // Transition corners 2x2
            slices[18] = new TextureRegion(tiles, startX + 16, startY + 16, 8, 8);
            slices[19] = new TextureRegion(tiles, startX + 24, startY + 16, 8, 8);
            slices[20] = new TextureRegion(tiles, startX + 16, startY + 24, 8, 8);
            slices[21] = new TextureRegion(tiles, startX + 24, startY + 24, 8, 8);

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
