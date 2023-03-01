package dev.michey.expo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.util.Pair;

import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class TileMapping {

    private TextureRegion[][] textureMappings;
    private float[][] chanceMappings;
    private HashMap<Integer, Pair<TextureRegion, Integer>> shadowMappings;

    public TextureRegion getRandom(int index) {
        if(chanceMappings == null) {
            return textureMappings[MathUtils.random(0, textureMappings.length - 1)][index];
        }

        float chance = MathUtils.random();
        float[] chances = chanceMappings[index]; // first tiles, then rows

        for(int i = 0; i < chances.length; i++) {
            if(chance <= chances[i]) return textureMappings[i][index];
        }

        return null;
    }

    public boolean supportsShadows(int id) {
        return shadowMappings != null && shadowMappings.containsKey(id);
    }

    public Pair<TextureRegion, Integer> getShadow(int index) {
        return shadowMappings.get(index);
    }

    public boolean load(String fileName) {
        try {
            FileHandle fh = Gdx.files.internal("textures/" + fileName);
            log("Loading TileMapping for " + fileName);
            String[] lines = fh.readString().split(System.lineSeparator());

            String textureName = lines[0].split("=")[1];
            int tiles = Integer.parseInt(lines[1].split("=")[1]);
            int rows = Integer.parseInt(lines[2].split("=")[1]);
            log("  textureName " + textureName);
            log("  tiles " + tiles);
            log("  rows " + rows);

            chanceMappings = new float[tiles][rows];

            for(int i = 3; i < 3 + rows; i++) {
                String currentLine = lines[i];
                String[] chances = currentLine.split(";");

                for(int tile = 0; tile < chances.length; tile++) {
                    float fChance = Float.parseFloat(chances[tile]);
                    chanceMappings[tile][i - 3] = fChance;
                }
            }

            textureMappings = new TextureRegion[rows][tiles];
            //TextureRegion baseTexture = ExpoAssets.get().textureRegion(textureName); // TO-DO: Change

            for(int i = 0; i < rows; i++) {
                for(int j = 0; j < tiles; j++) {
                    //textureMappings[i][j] = new TextureRegion(baseTexture, j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    int index = i * tiles + j;
                    textureMappings[i][j] = ExpoAssets.get().textureRegion(textureName + index);
                }
            }

            boolean supportsShadows = lines.length > (3 + rows);

            if(supportsShadows) {
                String shadowLine = lines[3 + rows];
                supportsShadows = Boolean.parseBoolean(shadowLine.split("=")[1]);

                if(supportsShadows) {
                    log("  shadowSupport = true");
                    shadowMappings = new HashMap<>();

                    for(int i = (3 + rows + 1); i < lines.length; i++) {
                        String[] raw = lines[i].split("=");
                        String[] rawValue = raw[1].split(",");
                        int id = Integer.parseInt(raw[0]);
                        int row = Integer.parseInt(rawValue[0]);
                        int offset = Integer.parseInt(rawValue[1]);

                        var p = new Pair<>(textureMappings[row][id], offset);
                        shadowMappings.put(id, p);
                    }
                }
            }

            if(!supportsShadows) {
                log("  shadowSupport = false");
            }

            log("Initialized " + (tiles * rows) + " tiles for " + fileName);
            return true;
        } catch (Exception e) {
            log("Failed to load TileMapping for " + fileName);
            e.printStackTrace();
        }

        return false;
    }

}