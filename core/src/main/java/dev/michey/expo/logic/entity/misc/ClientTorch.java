package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
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
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.util.ParticleBuilder;
import dev.michey.expo.util.ParticleColorMap;
import dev.michey.expo.util.ParticleEmitter;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class ClientTorch extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private TextureRegion torch;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;

    private final ExpoAnimation fireAnimation;
    private ExpoLight torchLight;
    private ParticleEmitter particleEmitter;
    private TrackedSoundData torchSound;

    public ClientTorch() {
        fireAnimation = new ExpoAnimation("flame", 11, 0.08f);
        fireAnimation.randomOffset();
    }

    @Override
    public void onCreation() {
        torch = tr("entity_torch");
        selectionTexture = generateSelectionTexture(torch);
        updateTextureBounds(9, 21, -3, 0);
        interactionPointArray = generateInteractionArray();
        updateDepth();
        torchLight = new ExpoLight(140f, 16, 1.0f, 0.3f, true);
        torchLight.color(0.992f, 0.541f, 0.184f, 1f);
        torchLight.setFlickering(6.0f, 0.15f);

        particleEmitter = new ParticleEmitter(new ParticleBuilder(ClientEntityType.PARTICLE_HIT)
                .amount(1, 2)
                .scale(0.3f, 0.7f)
                .lifetime(0.6f, 1.0f)
                .color(ParticleColorMap.of(14))
                .position(finalDrawPosX - 0.5f, finalDrawPosY + 13f)
                .velocity(-8, 8, 8, 28)
                .fadeout(0.6f)
                .randomRotation()
                .rotateWithVelocity()
                .textureRange(15, 15)
                .decreaseSpeed()
                .offset(4, 6)
                .depth(depth + 0.001f), 0.05f, 0.1f, 0.1f);
        particleEmitter.setLinkedEntity(this);
    }

    private void createSoundAndHandle() {
        float maxAudibleRange = PLAYER_AUDIO_RANGE * 0.125f;

        if(torchSound != null) {
            if(torchSound.postCalcVolume <= 0.0f) {
                AudioEngine.get().killSound(torchSound.id);
                torchSound = null;
            }
        } else {
            if(AudioEngine.get().dstPlayer(clientPosX, clientPosY + 13) < maxAudibleRange) {
                torchSound = AudioEngine.get().playSoundGroupManaged("campfire", new
                        Vector2(clientPosX, clientPosY + 13), maxAudibleRange, true, 0.125f);
            }
        }
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("wood_hit");

        if(newHealth <= 0) {
            //AudioEngine.get().playSoundGroupManaged("campfire_extinguish", new Vector2(clientPosX, clientPosY + torch.getRegionHeight() * 0.5f),
            //        PLAYER_AUDIO_RANGE * 0.45f, false, 0.6f);
        }

        ParticleSheet.Common.spawnWoodHitParticles(this, clientPosX, clientPosY + torch.getRegionHeight() * 0.5f);
        ParticleSheet.Common.spawnDustHitParticles(this);
    }

    @Override
    public void onDeletion() {
        torchLight.delete();
        if(torchSound != null) {
            AudioEngine.get().killSound(torchSound.id);
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
        torchLight.update(finalDrawPosX + 1.5f, finalDrawPosY + 13f, delta);
        particleEmitter.tick(delta);
        visibleToRenderEngine = RenderContext.get().inDrawBounds(this);
        createSoundAndHandle();

        if(visibleToRenderEngine) {
            fireAnimation.tick(delta);
        }
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
        rc.arraySpriteBatch.draw(fireAnimation.getFrame(), finalDrawPosX - 3.0f, finalDrawPosY + 9);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        if(visibleToRenderEngine) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(torch, finalDrawPosX, finalDrawPosY);
            rc.arraySpriteBatch.draw(fireAnimation.getFrame(), finalDrawPosX - 3.0f, finalDrawPosY + 9);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(torch, finalDrawPosX, finalDrawPosY, torch.getRegionWidth(), torch.getRegionHeight() * -1);
        TextureRegion frame = fireAnimation.getFrame();
        rc.arraySpriteBatch.draw(frame, finalDrawPosX - 3, finalDrawPosY - 9, frame.getRegionWidth(), frame.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX + 3, finalTextureStartY, 0, 0);
        float[] torchVertices = rc.arraySpriteBatch.obtainShadowVertices(torch, shadow);
        boolean drawShadow = rc.verticesInBounds(torchVertices);

        if(drawShadow) {
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.drawGradient(torch, torch.getRegionWidth(), torch.getRegionHeight(), shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.TORCH;
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO50(rc, 0.4f, 0.4f, 0, 0);
    }

}