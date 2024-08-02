package dev.michey.expo.logic.entity.container;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.assets.ParticleSheet;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.world.clientphysics.ClientPhysicsBody;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.shadow.ShadowUtils;

public class ClientCrate extends ClientEntity implements SelectableEntity, ReflectableEntity, AmbientOcclusionEntity {

    private TextureRegion texture;
    private TextureRegion shadowMask;
    private TextureRegion selectionTexture;
    private float[] interactionPointArray;

    private ClientPhysicsBody body;

    @Override
    public void onCreation() {
        disableTextureCentering = true;
        texture = tr("entity_crate");
        shadowMask = texture;
        selectionTexture = generateSelectionTexture(texture);
        updateTextureBounds(texture);
        interactionPointArray = generateInteractionArray(1);

        body = new ClientPhysicsBody(this, 0, 0, 17, 9.75f);
    }

    @Override
    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {
        playEntitySound("wood_hit");
        ParticleSheet.Common.spawnWoodHitParticles(this);
    }

    @Override
    public void onDeletion() {
        body.dispose();
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(RenderContext rc, float delta) {
        visibleToRenderEngine = rc.inDrawBounds(this);

        if(visibleToRenderEngine) {
            updateDepth();
            rc.useArrayBatch();
            rc.useRegularArrayShader();
            rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {
        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(shadowMask, shadow);
        boolean draw = rc.verticesInBounds(vertices);

        if(draw) {
            rc.arraySpriteBatch.drawGradient(shadowMask, textureWidth, textureHeight, shadow);
        }
    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.CRATE;
    }

    @Override
    public void renderSelected(RenderContext rc, float delta) {
        setSelectionValues();
        rc.arraySpriteBatch.draw(selectionTexture, finalSelectionDrawPosX, finalSelectionDrawPosY);
    }

    @Override
    public float[] interactionPoints() {
        return interactionPointArray;
    }

    @Override
    public void renderReflection(RenderContext rc, float delta) {
        rc.arraySpriteBatch.draw(texture, finalDrawPosX, finalDrawPosY, texture.getRegionWidth(), texture.getRegionHeight() * -1);
    }

    @Override
    public void renderAO(RenderContext rc) {
        drawAO100(rc, 0.45f, 0.5f, 8, 2);
    }

}
