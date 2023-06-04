package dev.michey.expo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;

import java.util.Arrays;
import java.util.HashMap;

public class TileMergerX {

    private Pixmap allTilesPixmap;

    public void prepare() {
        TextureRegion tiles = ExpoAssets.get().findTile("tile_rock_1");
        Texture tex = tiles.getTexture();
        if(!tex.getTextureData().isPrepared()) tex.getTextureData().prepare();
        allTilesPixmap = tex.getTextureData().consumePixmap();
        Gdx.files.external("convertedMergedTiles").mkdirs();
    }

    public void createFreshTile(int[] layerIds, String elevationTextureName, int variation) {
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
            TextureRegion elevationTexture = ExpoAssets.get().findTile(elevationTextureName);
            pixmap.drawPixmap(allTilesPixmap,
                    0, 16,
                    elevationTexture.getRegionX(),
                    elevationTexture.getRegionY(),
                    elevationTexture.getRegionWidth(),
                    elevationTexture.getRegionHeight());
        }

        writeToFile(pixmap, key);
        pixmap.dispose();
    }

    public HashMap<String, int[]> createAllPossibleVariations() {
        HashMap<String, int[]> possibleVariations = new HashMap<>();

        for(int i = 0; i < 256; i++) { // 2^8
            int tid = i / 16;
            int tis = i % 16;

            int[] ids = ServerTile.runTextureGrab(0, new int[] {tis, tid});
            possibleVariations.put(Arrays.toString(ids), ids);
        }

        ExpoLogger.log("Found " + possibleVariations.size() + " possible variations.");
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
        FileHandle fh = Gdx.files.external("convertedMergedTiles/" + name + ".png");
        PixmapIO.writePNG(fh, localPixmap);
    }

}
