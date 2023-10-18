package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

import java.util.HashMap;

public class ParticleSheet {

    private final HashMap<Integer, TextureRegion> particleTextureMap;
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

        public static void spawnGrassHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(10, 14)
                    .scale(0.3f, 0.8f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(1))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-24, 24, -24, 24)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(3, 7)
                    .decreaseSpeed()
                    .depth(entity.depth - 0.0001f)
                    .spawn();
        }

        public static void spawnBlueberryHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(10, 14)
                    .scale(0.3f, 0.8f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(9))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-24, 24, -24, 24)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(3, 7)
                    .decreaseSpeed()
                    .depth(entity.depth - 0.0001f)
                    .spawn();
        }

        public static void spawnMushroomHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(4, 7)
                    .scale(0.4f, 0.9f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(3))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-24, 24, -24, 24)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .depth(entity.depth - 0.0001f)
                    .decreaseSpeed()
                    .spawn();
        }

        public static void spawnRockHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(10, 14)
                    .scale(0.3f, 0.9f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(7))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-32, 32, -32, 32)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .decreaseSpeed()
                    .depth(entity.depth - 0.0001f)
                    .spawn();
        }

        public static void spawnBoulderHitParticles(ClientEntity entity, boolean coal) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(10, 14)
                    .scale(0.3f, 0.9f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(coal ? 8 : 7))
                    .position(entity.finalTextureStartX + 7.5f, entity.finalTextureStartY + 7f)
                    .velocity(-32, 32, -32, 32)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .decreaseSpeed()
                    .depth(entity.depth - 0.0001f)
                    .spawn();
        }

        public static void spawnTreeHitParticles(ClientEntity entity, float x, float y) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(10, 14)
                    .scale(0.3f, 0.9f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(5))
                    .position(x, y)
                    .velocity(-32, 32, -32, 32)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .decreaseSpeed()
                    .depth(entity.depth - 0.0001f)
                    .spawn();
        }

        public static void spawnWoodHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(3, 7)
                    .scale(0.6f, 0.8f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(6))
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