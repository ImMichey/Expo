package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.misc.ClientDynamic3DTile;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;

import java.util.HashMap;

import static dev.michey.expo.util.ExpoShared.TILE_SIZE;

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

        createParticle(sheet, 45, 0, 4, 4);
    }

    private void createParticle(TextureRegion baseSheet, int offsetX, int offsetY, int width, int height) {
        particleTextureMap.put(currentId, new TextureRegion(baseSheet, offsetX, offsetY, width, height));
        currentId++;
    }

    public TextureRegion getRandomParticle(int begin, int end) {
        return particleTextureMap.get(MathUtils.random(begin, end));
    }

    public static class Common {

        // This is manual due to texture passing
        public static void spawnGoreParticles(TextureRegion base, float x, float y) {
            if(!GameSettings.get().enableParticles) return;

            float bw = base.getRegionWidth();
            float bh = base.getRegionHeight();
            float total = bw * bh;
            int min = (int) (total * 0.07f);
            int max = (int) (total * 0.09f);

            new ParticleBuilder(ClientEntityType.PARTICLE_GORE)
                    .amount(min, max)
                    .scale(2.0f, 2.5f)
                    .lifetime(0.5f, 0.6f)
                    .position(x, y)
                    .velocityDirectional(160, 220)
                    .velocityCurve(Interpolation.pow3OutInverse)
                    .fadeoutLifetime(0.95f)
                    .texture(base)
                    .randomRotation()
                    .scaleDown()
                    .rotateWithVelocity()
                    .depth(y)
                    .spawn();
        }

        public static void spawnDustConstructFloorParticles(float x, float y) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(12, 18)
                    .scale(0.6f, 1.0f)
                    .lifetime(0.4f, 1.0f)
                    .color(ParticleColorMap.of(15))
                    .position(x, y)
                    .velocity(-32, 32, 4, 40)
                    .velocityCurve(Interpolation.pow3OutInverse)
                    .fadeoutLifetime(0.8f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .offset(16, 16)
                    .depth(y)
                    .spawn();
        }

        public static void spawnDustConstructEntityParticles(float x, float y, TextureRegion texture) {
            if(!GameSettings.get().enableParticles) return;
            float calculatedOffset = Math.max(texture.getRegionWidth() - 8, 2);
            float offset = (TILE_SIZE - calculatedOffset) * 0.5f;
            int plusParticles = (int) (calculatedOffset / 2);
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(4 + plusParticles, 8 + plusParticles)
                    .scale(0.6f, 1.0f)
                    .lifetime(0.4f, 1.0f)
                    .color(ParticleColorMap.of(15))
                    .position(x + offset, y + 1)
                    .velocity(-32, 32, 4, 40)
                    .velocityCurve(Interpolation.pow3OutInverse)
                    .fadeoutLifetime(0.8f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .offset(calculatedOffset, 2)
                    .depth(y + 1)
                    .spawn();
        }

        public static void spawnPlayerFootstepParticles(ClientPlayer entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(6, 9)
                    .scale(0.4f, 0.6f)
                    .lifetime(0.35f, 0.6f)
                    .color(ParticleColorMap.of(15))
                    .position(entity.finalTextureCenterX + 1 - (entity.direction() == 1 ? 0 : 7), entity.clientPosY)
                    .velocity(-32, 32, 32, 80)
                    .velocityCurve(Interpolation.pow3OutInverse)
                    .fadeout(0.35f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .offset(3, 0)
                    .depth(entity.depth + 0.001f)
                    .spawn();
        }

        public static void spawnExplosionParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(64, 96)
                    .scale(2.5f, 3.0f)
                    .lifetime(0.7f, 1.2f)
                    .color(ParticleColorMap.of(14))
                    .position(entity.finalTextureCenterX, entity.clientPosY)
                    //.velocity(-96, 96, 4, 112)
                    .velocityDirectional(8, 120)
                    .fadeoutLifetime(0.5f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .scaleDown()
                    .textureRange(15, 15)
                    .depth(entity.depth)
                    .spawn();
        }

        public static void spawnThrownDustParticles(ClientEntity entity, float heightOffset) {
            if(!GameSettings.get().enableParticles) return;
            float calculatedOffset = Math.max(entity.textureWidth - 8, 2);
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(8, 12)
                    .scale(0.6f, 1.0f)
                    .lifetime(0.4f, 1.0f)
                    .color(ParticleColorMap.of(15))
                    .position(entity.finalTextureCenterX - calculatedOffset * 0.5f, entity.clientPosY + heightOffset)
                    .velocity(-32, 32, 4, 40)
                    .velocityCurve(Interpolation.pow3OutInverse)
                    .fadeoutLifetime(0.8f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .offset(calculatedOffset, 0)
                    .depth(entity.depth)
                    .spawn();
        }

        public static void spawnDustHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            float calculatedOffset = Math.max(entity.textureWidth - 8, 2);
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(8, 12)
                    .scale(0.6f, 1.0f)
                    .lifetime(0.4f, 1.0f)
                    .color(ParticleColorMap.of(15))
                    .position(entity.finalTextureCenterX - calculatedOffset * 0.5f, entity.clientPosY)
                    .velocity(-32, 32, 4, 40)
                    .velocityCurve(Interpolation.pow3OutInverse)
                    .fadeoutLifetime(0.8f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .offset(calculatedOffset, 0)
                    .depth(entity.depth)
                    .spawn();
        }

        public static void spawnTorchParticles(float depth, float x, float y) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(1, 2)
                    .scale(0.3f, 0.7f)
                    .lifetime(0.6f, 1.0f)
                    .color(ParticleColorMap.of(14))
                    .position(x - 2, y - 4)
                    .velocity(-8, 8, 8, 28)
                    .fadeout(0.5f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .decreaseSpeed()
                    .offset(4, 6)
                    .depth(depth + 0.001f)
                    .spawn();
        }

        public static void spawnBloodParticles(ClientEntity entity, float ox, float oy) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(12, 16)
                    .scale(0.25f, 0.5f)
                    .lifetime(0.3f, 0.5f)
                    .color(ParticleColorMap.of(13))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-48, 48, -48, 48)
                    .fadeout(0.3f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .decreaseSpeed()
                    .depth(entity.depth - 0.001f)
                    .followEntity(entity)
                    .followOffset(ox, oy)
                    .spawn();
        }

        public static void spawnBloodParticlesWoodfolk(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(12, 16)
                    .scale(0.25f, 0.5f)
                    .lifetime(0.3f, 0.5f)
                    .color(ParticleColorMap.of(6))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-48, 48, -48, 48)
                    .fadeout(0.3f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .decreaseSpeed()
                    .depth(entity.depth - 0.001f)
                    .followEntity(entity)
                    .spawn();
        }

        public static void spawnBloodParticlesSlime(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(16, 20)
                    .scale(0.3f, 0.6f)
                    .lifetime(0.3f, 0.5f)
                    .color(ParticleColorMap.of(1))
                    .position(entity.finalTextureCenterX, entity.finalTextureCenterY)
                    .velocity(-48, 48, -48, 48)
                    .fadeout(0.3f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .textureRange(15, 15)
                    .decreaseSpeed()
                    .depth(entity.depth - 0.001f)
                    .followEntity(entity)
                    .spawn();
        }

        public static void spawnDynamic3DHitParticles(ClientDynamic3DTile cd3d) {
            if(!GameSettings.get().enableParticles) return;

            int colorId = 10;

            if(cd3d.emulatingType == TileLayerType.DIRT) {
                colorId = 11;
            } else if(cd3d.emulatingType == TileLayerType.OAKPLANKWALL) {
                colorId = 12;
            }

            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(12, 18)
                    .scale(0.3f, 0.7f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(colorId))
                    .position(cd3d.serverPosX + 2, cd3d.serverPosY + 2)
                    .offset(12, 28)
                    .velocity(-32, 32, -32, 32)
                    .fadein(0.1f)
                    .fadeout(0.1f)
                    .randomRotation()
                    .rotateWithVelocity()
                    .decreaseSpeed()
                    .depth(cd3d.depth - 0.001f)
                    .spawn();
        }

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
                    .depth(entity.depth - 0.001f)
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
                    .depth(entity.depth - 0.001f)
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
                    .depth(entity.depth - 0.001f)
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
                    .depth(entity.depth - 0.001f)
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
                    .depth(entity.depth - 0.001f)
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
                    .depth(entity.depth - 0.001f)
                    .spawn();
        }

        public static void spawnCampfireHitParticles(ClientEntity entity) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(3, 7)
                    .scale(0.6f, 0.8f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(6))
                    .position(entity.finalTextureStartX + 11.5f, entity.finalTextureStartY + 9.0f)
                    .velocity(-24, 24, -24, 24)
                    .fadeout(0.15f)
                    .textureRange(12, 14)
                    .randomRotation()
                    .rotateWithVelocity()
                    .depth(entity.depth - 0.001f)
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
                    .depth(entity.depth - 0.001f)
                    .spawn();
        }

        public static void spawnWoodHitParticles(ClientEntity entity, float x, float y) {
            if(!GameSettings.get().enableParticles) return;
            new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                    .amount(3, 7)
                    .scale(0.6f, 0.8f)
                    .lifetime(0.4f, 0.55f)
                    .color(ParticleColorMap.of(6))
                    .position(x, y)
                    .velocity(-24, 24, -24, 24)
                    .fadeout(0.15f)
                    .textureRange(12, 14)
                    .randomRotation()
                    .rotateWithVelocity()
                    .depth(entity.depth - 0.001f)
                    .spawn();
        }

    }

}