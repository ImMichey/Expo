package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientStarfish extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private TextureRegion texture;
    private float[] interactionPointArray;
    private TextureRegion selectionTexture;

    @Override
    public void onCreation() {
        texture = tr("entity_starfish");
        selectionTexture = generateSelectionTexture(texture);

        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray(2);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("stone_hit");
        ParticleSheet.Common.spawnRockHitParticles(this);
        ParticleSheet.Common.spawnDustHitParticles(this);
    }

    @Override
    public void onDeletion() {
        if(removalReason == EntityRemovalReason.DEATH) {
            playEntitySound("stone_break");
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
        rc.arraySpriteBatch.end();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth(5);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY + 12, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.25f, 0.9f, 0, 7.5f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.STARFISH;
    }

}