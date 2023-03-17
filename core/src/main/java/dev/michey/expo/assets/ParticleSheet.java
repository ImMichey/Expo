package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import java.util.HashMap;

public class ParticleSheet {

    private HashMap<Integer, TextureRegion> particleTextureMap;
    private int currentId;

    public ParticleSheet(TextureRegion sheet) {
        particleTextureMap = new HashMap<>();

        createParticle(sheet, 0, 0, 3, 3);
        createParticle(sheet, 4, 0, 3, 3);
        createParticle(sheet, 8, 0, 3, 3);
        createParticle(sheet, 12, 0, 2, 2);
        createParticle(sheet, 15, 0, 2, 2);
        createParticle(sheet, 18, 0, 2, 2);
        createParticle(sheet, 21, 0, 2, 2);
    }

    public HashMap<Integer, TextureRegion> getParticleTextureMap() {
        return particleTextureMap;
    }

    private void createParticle(TextureRegion baseSheet, int offsetX, int offsetY, int width, int height) {
        particleTextureMap.put(currentId, new TextureRegion(baseSheet, offsetX, offsetY, width, height));
        currentId++;
    }

    public TextureRegion randomHitParticle() {
        return particleTextureMap.get(MathUtils.random(0, 6));
    }

}