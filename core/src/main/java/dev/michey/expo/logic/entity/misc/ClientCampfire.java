package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.audio.TrackedSoundData;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ExpoAnimation;
import dev.michey.expo.render.light.ExpoLight;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;
import dev.michey.expo.util.ParticleEmitter;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class ClientCampfire extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private TextureRegion textureBase;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;

    private final ExpoAnimation fireAnimation;
    private boolean burning;
    private ParticleEmitter campfireSmokeEmitter;
    private ExpoLight campfireLight;
    private TrackedSoundData campfireSound;

    public ClientCampfire() {
        fireAnimation = new ExpoAnimation("largeflame", 18, 0.05f);
    }

    @Override
    public void onCreation() {
        textureBase = tr("entity_campfire");
        selectionTexture = generateSelectionTexture(textureBase);
        updateTextureBounds(textureBase);
        interactionPointArray = generateInteractionArray(3);

        updateDepth(4);
        campfireSmokeEmitter = new ParticleEmitter(
                new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                        .amount(2, 5)
                        .scale(0.45f, 0.85f)
                        .lifetime(0.6f, 1.2f)
                        .color(ParticleColorMap.of(14))
                        .position(finalDrawPosX + 10f, finalDrawPosY + 16f)
                        .velocity(-12, 12, 8, 34)
                        .fadeout(0.6f)
                        .randomRotation()
                        .rotateWithVelocity()
                        .textureRange(15, 15)
                        .decreaseSpeed()
                        .offset(5, 4)
                        .depth(depth + 0.001f), 0, 0.1f, 0.1f);
        campfireSmokeEmitter.setLinkedEntity(this);
    }

    private void constructLight() {
        if(!burning) {
            if(campfireLight != null) {
                campfireLight.delete();
                campfireLight = null;

                AudioEngine.get().playSoundGroupManaged("campfire_extinguish", new Vector2(clientPosX, clientPosY + 15), PLAYER_AUDIO_RANGE * 0.45f, false, 0.6f);
            }
        } else {
            if(campfireLight == null) {
                campfireLight = new ExpoLight(166, 16, 0.375f, 0.675f, true);
                campfireLight.color(0.992f, 0.541f, 0.184f, 1f);
                campfireLight.setFlickering(6.0f, 0.15f);
            }
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("wood_hit");

        ParticleSheet.Common.spawnCampfireHitParticles(this);

        if(newHealth <= 0) {
            ParticleSheet.Common.spawnDustHitParticles(this);
        }
    }

    @Override
    public void onDeletion() {
        if(campfireLight != null) {
            campfireLight.delete();

            if(campfireSound != null) {
                AudioEngine.get().killSound(campfireSound.id);
            }
        }
    }

    private void createSoundAndHandle(boolean shouldExist) {
        float maxAudibleRange = PLAYER_AUDIO_RANGE * 0.33f;

        if(campfireSound != null) {
            if(campfireSound.postCalcVolume <= 0.0f || !shouldExist) {
                AudioEngine.get().killSound(campfireSound.id);
                campfireSound = null;
            }
        } else if(shouldExist) {
            if(AudioEngine.get().dstPlayer(clientPosX, clientPosY + 15) < maxAudibleRange) {
                campfireSound = AudioEngine.get().playSoundGroupManaged("campfire", new
                        Vector2(clientPosX, clientPosY + 15), maxAudibleRange, true, 0.75f);
            }
        }
    }

    @Override
    public void applyCreationPayload(Object[] payload) {
        burning = ((float) payload[0]) > 0;
        fireAnimation.reset();
    }

    @Override
    public void applyEntityUpdatePayload(Object[] payload) {
        burning = ((float) payload[0]) > 0;
        fireAnimation.reset();
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        constructLight();
        createSoundAndHandle(burning);

        if(burning) {
            if(campfireLight != null) campfireLight.update(clientPosX, clientPosY + 15f, delta);
            fireAnimation.tick(delta);
            campfireSmokeEmitter.tick(delta);
        }
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();

        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();

        if(burning) {
            rc.arraySpriteBatch.draw(fireAnimation.getFrame(), finalDrawPosX + 7, finalDrawPosY + 11);
        }
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(4);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(textureBase, finalDrawPosX, finalDrawPosY);

            if(burning) {
                rc.arraySpriteBatch.draw(fireAnimation.getFrame(), finalDrawPosX + 7, finalDrawPosY + 11);
            }
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(textureBase, finalDrawPosX, finalDrawPosY, textureBase.getRegionWidth(), textureBase.getRegionHeight() * -1);

        if(burning) {
            TextureRegion f = fireAnimation.getFrame();
            rc.arraySpriteBatch.draw(f, finalDrawPosX + 7, finalDrawPosY - 11, f.getRegionWidth(), f.getRegionHeight() * -1);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(textureBase);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.4f, 0.6f, 0, 2f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CAMPFIRE;
    }

}