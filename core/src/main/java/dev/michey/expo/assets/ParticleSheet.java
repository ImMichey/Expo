package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

import java.util.HashMap;

public class ParticleSheet {

    private HashMap<Integer, TextureRegion> particleTextureMap;
    private int currentId;

    public ParticleSheet(TextureRegion sheet) {
        particleTextureMap = new HashMap<>();

        createParticle(sheet, 0, 0, 3, 3);
        createParticle(sheet, 4, 0, 3, 3);
        createParticle(sheet, 8, 0, 3, 3);

        createParticle(sheet, 12, 0, 1, 3);
        createParticle(sheet, 14, 0, 1, 2);
        createParticle(sheet, 16, 0, 1, 1);
        createParticle(sheet, 18, 0, 2, 3);
        createParticle(sheet, 21, 0, 2, 3);

        createParticle(sheet, 24, 0, 2, 2);
        createParticle(sheet, 27, 0, 2, 2);
        createParticle(sheet, 30, 0, 2, 2);
        createParticle(sheet, 33, 0, 2, 2);

        createParticle(sheet, 36, 0, 2, 3);
        createParticle(sheet, 39, 0, 2, 3);
        createParticle(sheet, 42, 0, 2, 3);
    }

    public HashMap<Integer, TextureRegion> getParticleTextureMap() {
        return particleTextureMap;
    }

    private void createParticle(TextureRegion baseSheet, int offsetX, int offsetY, int width, int height) {
        particleTextureMap.put(currentId, new TextureRegion(baseSheet, offsetX, offsetY, width, height));
        currentId++;
    }

    public TextureRegion randomHitParticle() {
        return particleTextureMap.get(MathUtils.random(0, 7));
    }

    public TextureRegion getRandomParticle(int begin, int end) {
        return particleTextureMap.get(MathUtils.random(begin, end));
    }

    public static class Common {

        public static void spawnWoodHitParticles(ClientEntity entity) {
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(3, 7)
                    .scale(0.6f, 0.8f)
                    .lifetime(0.3f, 0.5f)
                    .color(ParticleColorMap.random(6))
                    .position(entity.finalTextureStartX + 8, entity.finalTextureStartY + 8)
                    .velocity(-24, 24, -24, 24)
                    .fadeout(0.15f)
                    .textureRange(12, 14)
                    .randomRotation()
                    .rotateWithVelocity()
                    .depth(entity.depth - 0.0001f)
                    .spawn();
        }

    }

}
