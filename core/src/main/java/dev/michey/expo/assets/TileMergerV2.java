package dev.michey.expo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class TileMergerV2 {

    private Pixmap allTilesPixmap;

    private static final String OUTPUT_PATCH_FOLDER = "D:\\2021_09_25\\_SavedData\\SavedData06_12_19\\ExpoRes\\tiles\\_convertedPatchedTiles" + File.separator;

    public void prepare() {
        TextureRegion tiles = ExpoAssets.get().findTile("tile_rock_1");
        Texture tex = tiles.getTexture();
        if(!tex.getTextureData().isPrepared()) tex.getTextureData().prepare();
        allTilesPixmap = tex.getTextureData().consumePixmap();
    }

    public void createFreshTile(int[] layerIds, String elevationTextureName, int variation, int[] rawIds) {
        if(layerIds[0] == -1) return;

        StringBuilder builder = new StringBuilder();
        builder.append('t');

        for(int i = 0; i < layerIds.length; i++) {
            int n = layerIds[i];
            builder.append(n);
            if(i < (layerIds.length - 1)) {
                builder.append(',');
            }
        }

        if(elevationTextureName != null) {
            builder.append(',');
            builder.append(elevationTextureName);
        }

        if(variation >= 0) {
            builder.append(',');
            builder.append("variation_");
            builder.append(variation);
        }

        String key = builder.toString();

        int totalHeight = elevationTextureName == null ? 16 : 48;
        Pixmap pixmap = new Pixmap(16, totalHeight, Pixmap.Format.RGBA8888);

        if(layerIds.length == 1) {
            TextureRegion tex = getFrom(layerIds[0], variation);
            pixmap.drawPixmap(allTilesPixmap,
                    0, 0,
                    tex.getRegionX(),
                    tex.getRegionY(),
                    tex.getRegionWidth(),
                    tex.getRegionHeight());
        } else {
            TextureRegion tex0 = getFrom(layerIds[0], variation);
            TextureRegion tex1 = getFrom(layerIds[1], variation);
            TextureRegion tex2 = getFrom(layerIds[2], variation);
            TextureRegion tex3 = getFrom(layerIds[3], variation);

            pixmap.drawPixmap(allTilesPixmap,
                    0, 8,
                    tex0.getRegionX(),
                    tex0.getRegionY(),
                    tex0.getRegionWidth(),
                    tex0.getRegionHeight());

            pixmap.drawPixmap(allTilesPixmap,
                    8, 8,
                    tex1.getRegionX(),
                    tex1.getRegionY(),
                    tex1.getRegionWidth(),
                    tex1.getRegionHeight());

            pixmap.drawPixmap(allTilesPixmap,
                    0, 0,
                    tex2.getRegionX(),
                    tex2.getRegionY(),
                    tex2.getRegionWidth(),
                    tex2.getRegionHeight());

            pixmap.drawPixmap(allTilesPixmap,
                    8, 0,
                    tex3.getRegionX(),
                    tex3.getRegionY(),
                    tex3.getRegionWidth(),
                    tex3.getRegionHeight());
        }

        if(elevationTextureName != null) {
            if(rawIds[0] == 0) {
                ExpoLogger.log("-> " + elevationTextureName + " " + Arrays.toString(layerIds) + " " + layerIds[0]);
                TextureRegion elevationBase = getFrom(layerIds[0], variation);
                pixmap.drawPixmap(allTilesPixmap,
                        0, 16,
                        elevationBase.getRegionX(),
                        elevationBase.getRegionY(),
                        elevationBase.getRegionWidth(),
                        1);
            } else {
                TextureRegion elevationTexture = ExpoAssets.get().findTile(elevationTextureName);
                pixmap.drawPixmap(allTilesPixmap,
                        0, 16,
                        elevationTexture.getRegionX(),
                        elevationTexture.getRegionY(),
                        elevationTexture.getRegionWidth(),
                        elevationTexture.getRegionHeight());
            }
        }

        writeToFile(pixmap, key);
        pixmap.dispose();
    }

    public HashMap<String, int[]> createAllPossibleVariations() {
        HashMap<String, int[]> possibleVariations = new HashMap<>();

        int[][] handmade = new int[][] {
                new int[] {0},
                new int[] {1},
                new int[] {20, 5, 18, 11},
                new int[] {20, 5, 14, 19},
                new int[] {20, 5, 14, 11},
                new int[] {20, 5, 6, 3},
                new int[] {20, 9, 6, 7},
                new int[] {20, 21, 18, 19},
                new int[] {20, 21, 18, 11},
                new int[] {20, 21, 14, 19},
                new int[] {20, 21, 14, 11},
                new int[] {20, 21, 6, 3},
                new int[] {20, 9, 18, 15},
                new int[] {20, 9, 14, 15},
                new int[] {20, 5, 18, 19},
                new int[] {16, 17, 18, 15},
                new int[] {16, 17, 14, 15},
                new int[] {16, 17, 6, 7},
                new int[] {16, 13, 18, 19},
                new int[] {16, 13, 18, 11},
                new int[] {16, 13, 14, 19},
                new int[] {16, 13, 14, 11},
                new int[] {16, 13, 6, 3},
                new int[] {12, 17, 10, 15},
                new int[] {12, 17, 2, 7},
                new int[] {12, 13, 10, 19},
                new int[] {12, 13, 10, 11},
                new int[] {12, 13, 2, 3},
                new int[] {8, 21, 18, 19},
                new int[] {8, 21, 18, 11},
                new int[] {8, 21, 14, 19},
                new int[] {8, 21, 14, 11},
                new int[] {8, 21, 6, 3},
                new int[] {8, 9, 18, 15},
                new int[] {8, 9, 14, 15},
                new int[] {8, 9, 6, 7},
                new int[] {8, 5, 18, 19},
                new int[] {8, 5, 18, 11},
                new int[] {8, 5, 14, 19},
                new int[] {8, 5, 6, 3},
                new int[] {4, 21, 10, 19},
                new int[] {4, 21, 10, 11},
                new int[] {4, 21, 2, 3},
                new int[] {4, 9, 10, 15},
                new int[] {4, 9, 2, 7},
                new int[] {4, 5, 10, 19},
                new int[] {4, 5, 10, 11},
                new int[] {4, 5, 2, 3}
        };

        for(int[] v : handmade) possibleVariations.put(Arrays.toString(v), v);
        return possibleVariations;
    }

    private TextureRegion getFrom(int id, int variation) {
        if(variation >= 0) {
            return ExpoAssets.get().getTileSheet().getVariation(id, variation);
        } else {
            return ExpoAssets.get().getTileSheet().getTilesetTextureMap().get(id);
        }
    }

    public void writeToFile(Pixmap localPixmap, String name) {
        FileHandle fh = Gdx.files.absolute(OUTPUT_PATCH_FOLDER + name + ".png");
        PixmapIO.writePNG(fh, localPixmap);
    }

}
