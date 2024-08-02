package dev.michey.expo.logic.entity.flora;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientMushroomBrown extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private TextureRegion texture;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;

    @Override
    public void onCreation() {
        texture = tr("entity_mushroom_brownn");
        selectionTexture = generateSelectionTexture(texture);
        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray();
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("grass_hit");

        ParticleSheet.Common.spawnMushroomHitParticles(this);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("harvest");
        }
    }

    @Override
    public void tick(float delta) {
        syncPositionWithServer();
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(textureOffsetY);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        drawShadowIfVisible(texture);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.225f, 0.35f, 0, 0.5f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.MUSHROOM_BROWN;
    }

}