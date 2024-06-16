package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.util.EntityRemovalReason;

public class ClientRock extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private int variant;
    private TextureRegion texture;
    private float[] interactionPointArray;
    private TextureRegion selectionTexture;

    @Override
    public void onCreation() {
        texture = new TextureRegion(tr("entity_rockn_" + variant));
        selectionTexture = generateSelectionTexture(texture);

        if(MathUtils.randomBoolean()) {
            texture.flip(true, false);
            selectionTexture.flip(true, false);
        }

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
    public void applyCreationPayload(Object[] payload) {
        variant = (int) payload[0];
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
            updateDepth(1);
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY + 2, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.225f, 0.375f, -0.5f, 2f);
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.ROCK;
    }

}