package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Arrays;
import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class TileSheet {

    public final TextureRegion baseSoil;
    public final TextureRegion notSet;
    private HashMap<Integer, TextureRegion> tilesetTextureMap;
    private int currentId;

    public TileSheet(TextureRegion sheet) {
        tilesetTextureMap = new HashMap<>();

        baseSoil = new TextureRegion(sheet, 0, 0, 16, 16);
        tilesetTextureMap.put(0, baseSoil);
        currentId = 1;

        create(sheet, 0, 16, "GRASS");
        create(sheet, 0, 48, "SAND");

        notSet = new TextureRegion(sheet, 0, 80, 16, 16);
        tilesetTextureMap.put(currentId, notSet);
        currentId++;

        create(sheet, 0, 96, "OCEAN");
        create(sheet, 0, 128, "OCEAN_DEEP");
    }

    public HashMap<Integer, TextureRegion> getTilesetTextureMap() {
        return tilesetTextureMap;
    }

    private void create(TextureRegion baseSheet, int offsetX, int offsetY, String identifier) {
        final int TEXTURES = 22;
        TextureRegion[] sheet = new TextureRegion[TEXTURES];
        int[] ids = new int[TEXTURES];
        for(int i = 0; i < TEXTURES; i++) {
            ids[i] = currentId + i;
        }
        currentId += TEXTURES;

        // Full all sides texture
        sheet[0] = new TextureRegion(baseSheet, offsetX, offsetY, 16, 16);

        // Single texture
        sheet[1] = new TextureRegion(baseSheet, offsetX + 16, offsetY, 16, 16);

        // Top Left Corner 2x2
        sheet[2] = new TextureRegion(baseSheet, offsetX + 32, offsetY, 8, 8);
        sheet[3] = new TextureRegion(baseSheet, offsetX + 40, offsetY, 8, 8);
        sheet[4] = new TextureRegion(baseSheet, offsetX + 32, offsetY + 8, 8, 8);
        sheet[5] = new TextureRegion(baseSheet, offsetX + 40, offsetY + 8, 8, 8);

        // Top Right Corner 2x2
        sheet[6] = new TextureRegion(baseSheet, offsetX + 48, offsetY, 8, 8);
        sheet[7] = new TextureRegion(baseSheet, offsetX + 56, offsetY, 8, 8);
        sheet[8] = new TextureRegion(baseSheet, offsetX + 48, offsetY + 8, 8, 8);
        sheet[9] = new TextureRegion(baseSheet, offsetX + 56, offsetY + 8, 8, 8);

        // Bottom Left Corner 2x2
        sheet[10] = new TextureRegion(baseSheet, offsetX + 32, offsetY + 16, 8, 8);
        sheet[11] = new TextureRegion(baseSheet, offsetX + 40, offsetY + 16, 8, 8);
        sheet[12] = new TextureRegion(baseSheet, offsetX + 32, offsetY + 24, 8, 8);
        sheet[13] = new TextureRegion(baseSheet, offsetX + 40, offsetY + 24, 8, 8);

        // Bottom Right Corner 2x2
        sheet[14] = new TextureRegion(baseSheet, offsetX + 48, offsetY + 16, 8, 8);
        sheet[15] = new TextureRegion(baseSheet, offsetX + 56, offsetY + 16, 8, 8);
        sheet[16] = new TextureRegion(baseSheet, offsetX + 48, offsetY + 24, 8, 8);
        sheet[17] = new TextureRegion(baseSheet, offsetX + 56, offsetY + 24, 8, 8);

        // Transition corners 2x2
        sheet[18] = new TextureRegion(baseSheet, offsetX + 16, offsetY + 16, 8, 8);
        sheet[19] = new TextureRegion(baseSheet, offsetX + 24, offsetY + 16, 8, 8);
        sheet[20] = new TextureRegion(baseSheet, offsetX + 16, offsetY + 24, 8, 8);
        sheet[21] = new TextureRegion(baseSheet, offsetX + 24, offsetY + 24, 8, 8);

        for(int i = 0; i < TEXTURES; i++) {
            tilesetTextureMap.put(i + currentId - TEXTURES, sheet[i]);
        }

        log("Created new Tileset, id range: " + Arrays.toString(ids));
    }

}
