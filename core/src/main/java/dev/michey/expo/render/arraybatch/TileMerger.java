package dev.michey.expo.render.arraybatch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.TileLayerType;

import java.util.HashMap;

public class TileMerger {

    private static TileMerger INSTANCE;

    private final HashMap<String, TextureRegion> tileMap;
    private final Pixmap allTilesPixmap;

    private final Pixmap combinedTilesPixmap;
    private Texture combinedTilesTexture;
    private final int TEXTURE_SIZE = 2048;
    private int currentX;
    private int currentY;

    public TileMerger() {
        tileMap = new HashMap<>();

        TextureData textureData = ExpoAssets.get().findTile("tile_rock_elevation_1").getTexture().getTextureData();
        if(!textureData.isPrepared()) textureData.prepare();
        allTilesPixmap = textureData.consumePixmap();

        combinedTilesPixmap = new Pixmap(TEXTURE_SIZE, TEXTURE_SIZE, Pixmap.Format.RGBA8888);
    }

    public void createConnectedTiles() {
        int[] bottomLeft = new int[] {4, 8, 12, 16, 20};
        int[] bottomRight = new int[] {5, 9, 13, 17, 21};
        int[] topLeft = new int[] {2, 6, 10, 14, 18};
        int[] topRight = new int[] {3, 7, 11, 15, 19};

        for(TileLayerType tlt : TileLayerType.values()) {
            int minTile = tlt.TILE_ID_DATA[0];
            if(tlt.TILE_ID_DATA[0] == -1) continue;

            for(int bL : bottomLeft) {
                for(int bR : bottomRight) {
                    for(int tL : topLeft) {
                        for(int tR : topRight) {
                            int[] ids = new int[] {bL + minTile, bR + minTile, tL + minTile, tR + minTile};
                            getCombinedTile(ids, null, -1, false);
                        }
                    }
                }
            }

            int[] seriesSpecial = new int[] {20 + minTile, 21 + minTile, 18 + minTile, 19 + minTile};

            getCombinedTile(new int[] {minTile}, null, -1, false);

            int variations = ExpoAssets.get().getTileSheet().getAmountOfVariations(minTile);

            if(variations > 0) {
                for(int i = 0; i < variations; i++) {
                    getCombinedTile(new int[] {minTile}, null, i, false);
                }
            }

            getCombinedTile(new int[] {1 + minTile}, null, -1, false);
            getCombinedTile(seriesSpecial, null, -1, false);
        }
        combinedTilesTexture.draw(combinedTilesPixmap, 0, 0);

        PixmapIO.writePNG(Gdx.files.external("test/PIXMAP_FINAL_" + System.currentTimeMillis() + ".png"), combinedTilesPixmap);
    }

    private TextureRegion getFrom(int id, int variation) {
        if(variation >= 0) {
            return ExpoAssets.get().getTileSheet().getVariation(id, variation);
        } else {
            return ExpoAssets.get().getTileSheet().getTilesetTextureMap().get(id);
        }
    }

    public TextureRegion getCombinedTile(int[] layerIds, String elevationTextureName, int variation, boolean updateTexture) {
        if(layerIds[0] == -1) return null;

        // Create a TextureRegion key to hash in map
        StringBuilder builder = new StringBuilder();
        for(int i : layerIds) {
            builder.append(i);
            builder.append(',');
            if(elevationTextureName != null) {
                builder.append(',');
                builder.append(elevationTextureName);
            }
            if(variation >= 0) {
                builder.append(',');
                builder.append("variation_");
                builder.append(variation);
            }
        }
        String key = builder.toString();
        TextureRegion found = tileMap.get(key);
        if(found != null) return found;

        // Create a new pixmap
        int totalHeight = elevationTextureName == null ? 16 : 48;
        int padding = 2;
        int paddingReal = 2;
        Pixmap pixmap = new Pixmap(16 + paddingReal * 2, totalHeight + paddingReal * 2, Pixmap.Format.RGBA8888);

        // Write to newly created pixmap
        if(layerIds.length == 1) {
            TextureRegion tex = getFrom(layerIds[0], variation);
            pixmap.drawPixmap(allTilesPixmap,
                    0, 0,
                    tex.getRegionX() - paddingReal,
                    tex.getRegionY() - paddingReal,
                    tex.getRegionWidth() + paddingReal * 2,
                    tex.getRegionHeight() + paddingReal * 2);
        } else {
            TextureRegion tex0 = getFrom(layerIds[0], variation);
            TextureRegion tex1 = getFrom(layerIds[1], variation);
            TextureRegion tex2 = getFrom(layerIds[2], variation);
            TextureRegion tex3 = getFrom(layerIds[3], variation);

            pixmap.drawPixmap(allTilesPixmap,
                    0, 8 + paddingReal,
                    tex0.getRegionX() - paddingReal,
                    tex0.getRegionY(),
                    tex0.getRegionWidth() + paddingReal,
                    tex0.getRegionHeight() + paddingReal);

            pixmap.drawPixmap(allTilesPixmap,
                    8 + paddingReal, 8 + paddingReal,
                    tex1.getRegionX(),
                    tex1.getRegionY(),
                    tex1.getRegionWidth() + paddingReal,
                    tex1.getRegionHeight() + paddingReal);

            pixmap.drawPixmap(allTilesPixmap,
                    0, 0,
                    tex2.getRegionX() - paddingReal,
                    tex2.getRegionY() - paddingReal,
                    tex2.getRegionWidth() + paddingReal,
                    tex2.getRegionHeight() + paddingReal);

            pixmap.drawPixmap(allTilesPixmap,
                    8 + paddingReal, 0,
                    tex3.getRegionX(),
                    tex3.getRegionY() - paddingReal,
                    tex3.getRegionWidth() + paddingReal,
                    tex3.getRegionHeight() + paddingReal);
        }
        if(elevationTextureName != null) {
            TextureRegion elevationTexture = ExpoAssets.get().findTile(elevationTextureName);
            pixmap.drawPixmap(allTilesPixmap,
                    0, 16 + paddingReal,
                    elevationTexture.getRegionX() - paddingReal,
                    elevationTexture.getRegionY(),
                    elevationTexture.getRegionWidth() + paddingReal * 2,
                    elevationTexture.getRegionHeight() + paddingReal);
        }

        // Do line breaks within the pixmap/texture
        if(currentX >= (TEXTURE_SIZE + 16 + paddingReal + paddingReal * 2)) {
            currentX = 0;
            currentY += totalHeight + padding + paddingReal * 2;
        }

        // Update combined tiles pixmap
        combinedTilesPixmap.drawPixmap(pixmap, currentX + padding, currentY + padding);

        if(combinedTilesTexture == null) {
            combinedTilesTexture = new Texture(combinedTilesPixmap);
        } else {
            if(updateTexture) {
                combinedTilesTexture.draw(combinedTilesPixmap, 0, 0);
            }
        }

        // Delete pixmap after writing
        pixmap.dispose();

        // Generate TextureRegion and cache result
        TextureRegion tr = new TextureRegion(combinedTilesTexture, currentX + padding + paddingReal, currentY + padding + paddingReal, 16, totalHeight);
        tileMap.put(key, tr);
        currentX += 16 + padding + paddingReal * 2;

        return tr;
    }

    public static TileMerger get() {
        if(INSTANCE == null) INSTANCE = new TileMerger();
        return INSTANCE;
    }

}
